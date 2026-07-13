package com.joao.storemanagement.serviceImpl.system;

import cn.hutool.core.util.StrUtil;
import com.joao.storemanagement.config.StoreProperties;
import com.joao.storemanagement.dto.system.DependencyHealthDTO;
import com.joao.storemanagement.dto.system.SystemHealthReportDTO;
import com.joao.storemanagement.mapper.primary.SystemHealthMapper;
import com.joao.storemanagement.service.system.SystemHealthService;
import com.joao.storemanagement.service.talent.TalentOposDataSourceService;
import com.joao.storemanagement.vo.talent.DataSourceStatusVO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.stereotype.Service;

@Service
public class SystemHealthServiceImpl implements SystemHealthService {

    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";
    private static final String STATUS_WARN = "WARN";

    private final DataSource dataSource;
    private final StoreProperties storeProperties;
    private final TalentOposDataSourceService talentOposDataSourceService;
    private final SystemHealthMapper systemHealthMapper;

    public SystemHealthServiceImpl(
            DataSource dataSource,
            StoreProperties storeProperties,
            TalentOposDataSourceService talentOposDataSourceService,
            SystemHealthMapper systemHealthMapper) {
        this.dataSource = dataSource;
        this.storeProperties = storeProperties;
        this.talentOposDataSourceService = talentOposDataSourceService;
        this.systemHealthMapper = systemHealthMapper;
    }

    @Override
    public SystemHealthReportDTO report() {
        List<DependencyHealthDTO> dependencies = List.of(
                checkPrimaryDatabase(),
                checkTalentOpos(),
                checkGoogleDrive(),
                checkDirectory("attachmentDirectory", storeProperties.getUpload().getAttachmentDir(), "附件上传目录"),
                checkDirectory("invoiceArchiveDirectory", storeProperties.getUpload().getInvoiceArchiveDir(), "发票归档目录"));
        return new SystemHealthReportDTO(
                dependencies,
                systemHealthMapper.selectRecentApiErrors(),
                systemHealthMapper.selectRecentImportFailures(),
                systemHealthMapper.selectRecentSyncIssues());
    }

    private DependencyHealthDTO checkPrimaryDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                return new DependencyHealthDTO("primaryDatabase", STATUS_UP, "主库连接正常");
            }
            return new DependencyHealthDTO("primaryDatabase", STATUS_DOWN, "主库连接不可用");
        } catch (Exception ex) {
            return new DependencyHealthDTO("primaryDatabase", STATUS_DOWN, "主库连接失败: " + ex.getMessage());
        }
    }

    private DependencyHealthDTO checkTalentOpos() {
        try {
            DataSourceStatusVO status = talentOposDataSourceService.status();
            if (!status.isConfigured()) {
                return new DependencyHealthDTO("talentOpos", STATUS_WARN, status.getMessage());
            }
            if (status.isConnected()) {
                return new DependencyHealthDTO("talentOpos", STATUS_UP, status.getMessage());
            }
            return new DependencyHealthDTO("talentOpos", STATUS_DOWN, status.getMessage());
        } catch (Exception ex) {
            return new DependencyHealthDTO("talentOpos", STATUS_DOWN, "TALENTOPOS 检查失败: " + ex.getMessage());
        }
    }

    private DependencyHealthDTO checkGoogleDrive() {
        StoreProperties.GoogleDrive config = storeProperties.getGoogleDrive();
        if (!config.isEnabled()) {
            return new DependencyHealthDTO("googleDrive", STATUS_WARN, "Google Drive 未启用");
        }
        List<String> missing = new ArrayList<>();
        if (StrUtil.isBlank(config.getClientId())) {
            missing.add("Client ID");
        }
        if (StrUtil.isBlank(config.getClientSecret())) {
            missing.add("Client Secret");
        }
        if (StrUtil.isBlank(config.getTokenPath())) {
            missing.add("Token 路径");
        } else if (!Files.isRegularFile(Path.of(config.getTokenPath()))) {
            missing.add("Token 文件不存在");
        }
        if (missing.isEmpty()) {
            return new DependencyHealthDTO("googleDrive", STATUS_UP, "Google Drive 配置完整");
        }
        return new DependencyHealthDTO("googleDrive", STATUS_WARN, "配置缺失: " + String.join("、", missing));
    }

    private DependencyHealthDTO checkDirectory(String component, String dir, String label) {
        if (StrUtil.isBlank(dir)) {
            return new DependencyHealthDTO(component, STATUS_DOWN, label + "未配置");
        }
        Path path = Path.of(dir);
        try {
            Files.createDirectories(path);
            if (!Files.isWritable(path)) {
                return new DependencyHealthDTO(component, STATUS_DOWN, label + "不可写: " + dir);
            }
            return new DependencyHealthDTO(component, STATUS_UP, label + "正常");
        } catch (Exception ex) {
            return new DependencyHealthDTO(component, STATUS_DOWN, label + "不可用: " + ex.getMessage());
        }
    }
}
