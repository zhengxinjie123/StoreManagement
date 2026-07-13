package com.joao.storemanagement.serviceImpl.system;

import cn.hutool.core.util.StrUtil;
import com.joao.storemanagement.config.StoreProperties;
import com.joao.storemanagement.entity.primary.TalentDataSourceConfig;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.mapper.primary.TalentDataSourceConfigMapper;
import com.joao.storemanagement.service.system.DatabaseResetService;
import com.joao.storemanagement.system.DatabaseResetConstants;
import com.joao.storemanagement.vo.system.DatabaseResetResultVO;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseResetServiceImpl implements DatabaseResetService {

    private static final long TALENT_CONFIG_ID = 1L;
    private static final Pattern DATABASE_NAME_PATTERN =
            Pattern.compile("databaseName=([^;]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SAFE_DATABASE_NAME_PATTERN =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final DateTimeFormatter BACKUP_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.ROOT);

    private final DataSource dataSource;
    private final StoreProperties storeProperties;
    private final TalentDataSourceConfigMapper talentDataSourceConfigMapper;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${store.database.backup-dir:backups/db}")
    private String backupDir;

    @Value("${store.database.reset-target-name:" + DatabaseResetConstants.TARGET_DATABASE + "}")
    private String resetTargetDatabaseName;

    @Override
    public DatabaseResetResultVO resetAllData(String confirmPassword) {
        if (!DatabaseResetConstants.CONFIRM_PASSWORD.equals(confirmPassword)) {
            throw new BusinessException("确认口令错误");
        }

        String databaseName = resolveDatabaseName();
        validateResetScope(databaseName);

        Path backupDirectory = resolveBackupDirectory();
        String backupFileName = databaseName + "_" + LocalDateTime.now().format(BACKUP_TIME_FORMAT) + ".bak";
        Path backupFilePath = backupDirectory.resolve(backupFileName).toAbsolutePath().normalize();

        createFullBackup(databaseName, backupFilePath);
        List<Path> uploadDirectories = resolveUploadDirectories();
        int clearedTableCount = clearStoreManagementTables(databaseName);
        clearUploadDirectories(uploadDirectories);

        long backupSize = 0L;
        try {
            backupSize = Files.size(backupFilePath);
        } catch (IOException ex) {
            log.warn("读取备份文件大小失败: {}", backupFilePath, ex);
        }

        String protectedTalentDatabase = resolveProtectedTalentDatabaseName();
        log.warn("Store_Management 库已重置: database={}, backup={}, clearedTables={}, protectedTables={}, protectedTalentDatabase={}",
                databaseName, backupFilePath, clearedTableCount, DatabaseResetConstants.PROTECTED_TABLES, protectedTalentDatabase);

        return DatabaseResetResultVO.builder()
                .backupFilePath(backupFilePath.toString())
                .backupFileName(backupFileName)
                .backupSizeBytes(backupSize)
                .clearedTableCount(clearedTableCount)
                .message("已完成 "
                        + databaseName
                        + " 库全库备份并清空业务数据。系统参数表（app_config）已保留。"
                        + " TALENTOPOS 外部数据源"
                        + (StrUtil.isNotBlank(protectedTalentDatabase)
                                ? "（" + protectedTalentDatabase + "）"
                                : "")
                        + "未被修改。请使用默认管理员账号 admin / admin123 重新登录。")
                .build();
    }

    private String resolveDatabaseName() {
        if (StrUtil.isBlank(datasourceUrl)) {
            throw new BusinessException("未配置数据库连接");
        }
        Matcher matcher = DATABASE_NAME_PATTERN.matcher(datasourceUrl);
        if (!matcher.find()) {
            throw new BusinessException("无法从数据源 URL 解析数据库名");
        }
        return matcher.group(1).trim();
    }

    private void validateResetScope(String databaseName) {
        if (!databaseName.equalsIgnoreCase(resetTargetDatabaseName)) {
            throw new BusinessException("仅允许重置主库 "
                    + resetTargetDatabaseName
                    + "，当前连接为 "
                    + databaseName);
        }
        if (!SAFE_DATABASE_NAME_PATTERN.matcher(databaseName).matches()) {
            throw new BusinessException("数据库名非法，已中止重置: " + databaseName);
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String currentDatabase = jdbcTemplate.queryForObject("SELECT DB_NAME()", String.class);
        if (StrUtil.isBlank(currentDatabase) || !currentDatabase.equalsIgnoreCase(databaseName)) {
            throw new BusinessException("当前 JDBC 连接库为 "
                    + currentDatabase
                    + "，与目标库 "
                    + databaseName
                    + " 不一致，已中止重置");
        }

        TalentDataSourceConfig talentConfig = talentDataSourceConfigMapper.selectById(TALENT_CONFIG_ID);
        if (talentConfig != null
                && StrUtil.isNotBlank(talentConfig.getDatabaseName())
                && talentConfig.getDatabaseName().equalsIgnoreCase(databaseName)) {
            throw new BusinessException("TALENTOPOS 数据源与主库同名（"
                    + talentConfig.getDatabaseName()
                    + "），禁止重置以避免误删 POS 数据");
        }
    }

    private String resolveProtectedTalentDatabaseName() {
        TalentDataSourceConfig talentConfig = talentDataSourceConfigMapper.selectById(TALENT_CONFIG_ID);
        if (talentConfig == null || StrUtil.isBlank(talentConfig.getDatabaseName())) {
            return null;
        }
        return talentConfig.getDatabaseName();
    }

    private String quotedDatabaseName(String databaseName) {
        return "[" + databaseName + "]";
    }

    private Path resolveBackupDirectory() {
        try {
            Path directory = Path.of(backupDir).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            return directory;
        } catch (IOException ex) {
            throw new BusinessException("创建备份目录失败: " + ex.getMessage(), ex);
        }
    }

    private void createFullBackup(String databaseName, Path backupFilePath) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String escapedPath = backupFilePath.toString().replace("'", "''");
        String sql = "BACKUP DATABASE " + quotedDatabaseName(databaseName) + " TO DISK = N'"
                + escapedPath
                + "' WITH FORMAT, INIT, NAME = N'"
                + databaseName
                + "_full_backup', SKIP, NOREWIND, NOUNLOAD, STATS = 10";
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ex) {
            throw new BusinessException(
                    "全库备份失败，请确认 SQL Server 服务账号对目录有写权限: "
                            + backupFilePath.getParent()
                            + "。原因: "
                            + ex.getMessage(),
                    ex);
        }
        if (!Files.exists(backupFilePath)) {
            throw new BusinessException("全库备份失败，未生成备份文件: " + backupFilePath);
        }
    }

    private int clearStoreManagementTables(String databaseName) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String quotedDatabase = quotedDatabaseName(databaseName);
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT QUOTENAME(s.name) + '.' + QUOTENAME(t.name)
                FROM sys.tables t
                INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE t.is_ms_shipped = 0 AND s.name = 'dbo'
                ORDER BY t.name
                """, String.class);
        List<String> tablesToClear = tables.stream()
                .filter(this::shouldClearTable)
                .toList();
        if (tablesToClear.isEmpty()) {
            return 0;
        }

        for (String table : tablesToClear) {
            jdbcTemplate.execute("ALTER TABLE " + quotedDatabase + "." + table + " NOCHECK CONSTRAINT ALL");
        }
        for (String table : tablesToClear) {
            jdbcTemplate.execute("DELETE FROM " + quotedDatabase + "." + table);
        }
        for (String table : tablesToClear) {
            jdbcTemplate.execute("ALTER TABLE " + quotedDatabase + "." + table + " WITH CHECK CHECK CONSTRAINT ALL");
        }
        return tablesToClear.size();
    }

    private boolean shouldClearTable(String quotedTableName) {
        String tableName = quotedTableName.replace("[", "").replace("]", "");
        int dotIndex = tableName.lastIndexOf('.');
        if (dotIndex >= 0) {
            tableName = tableName.substring(dotIndex + 1);
        }
        return !DatabaseResetConstants.PROTECTED_TABLES.contains(tableName);
    }

    private List<Path> resolveUploadDirectories() {
        List<Path> directories = new ArrayList<>();
        directories.add(Path.of(storeProperties.getUpload().getAttachmentDir()));
        directories.add(Path.of(storeProperties.getUpload().getInvoiceArchiveDir()));
        directories.add(Path.of(storeProperties.getEmail().getStorageDir()));
        directories.add(Path.of("uploads"));
        return directories.stream()
                .map(path -> path.toAbsolutePath().normalize())
                .distinct()
                .toList();
    }

    private void clearUploadDirectories(List<Path> directories) {
        for (Path directory : directories) {
            deleteDirectoryContents(directory);
        }
    }

    private void deleteDirectoryContents(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (!dir.equals(directory)) {
                        Files.deleteIfExists(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            log.warn("清理目录失败: {}", directory, ex);
        }
    }
}
