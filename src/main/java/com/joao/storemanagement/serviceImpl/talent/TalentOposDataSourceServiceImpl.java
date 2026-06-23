package com.joao.storemanagement.serviceImpl.talent;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.joao.storemanagement.dto.talent.DataSourceDTO;
import com.joao.storemanagement.entity.primary.TalentDataSourceConfig;
import com.joao.storemanagement.entity.talent.Supplier;
import com.joao.storemanagement.exceptions.BusinessException;
import com.joao.storemanagement.mapper.primary.TalentDataSourceConfigMapper;
import com.joao.storemanagement.mapper.talent.ProductMapper;
import com.joao.storemanagement.mapper.talent.SupplierMapper;
import com.joao.storemanagement.security.TextEncryptor;
import com.joao.storemanagement.service.talent.TalentOposDataSourceService;
import com.joao.storemanagement.utils.GuidHelper;
import com.joao.storemanagement.vo.talent.DataSourceStatusVO;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TalentOposDataSourceServiceImpl implements TalentOposDataSourceService {

    private static final long CONFIG_ID = 1L;
    private static final String DRIVER_CLASS = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    private final TalentDataSourceConfigMapper talentDataSourceConfigMapper;

    private volatile HikariDataSource dataSource;
    private volatile SqlSessionFactory sqlSessionFactory;
    private volatile DataSourceDTO config;
    private volatile LocalDateTime lastAppliedAt;

    @Value("${store.security.datasource-config-key}")
    private String datasourceConfigKey;

    @PostConstruct
    public void initFromSavedConfig() {
        TalentDataSourceConfig savedConfig;
        try {
            savedConfig = talentDataSourceConfigMapper.selectById(CONFIG_ID);
        } catch (RuntimeException ex) {
            log.warn("读取 TALENTOPOS 数据源保存配置失败，跳过自动恢复: {}", ex.getMessage());
            return;
        }
        if (savedConfig == null) {
            log.info("TALENTOPOS 数据源尚未保存配置");
            return;
        }
        try {
            apply(toRequest(savedConfig), false);
            lastAppliedAt = savedConfig.getUpdatedAt();
            log.info("TALENTOPOS 数据源已从主库配置恢复: {}:{}/{}",
                    savedConfig.getHost(), savedConfig.getPort(), savedConfig.getDatabaseName());
        } catch (SQLException ex) {
            lastAppliedAt = savedConfig.getUpdatedAt();
            log.warn("TALENTOPOS 数据源配置已加载，但连接失败: {}", ex.getMessage());
        } catch (RuntimeException ex) {
            lastAppliedAt = savedConfig.getUpdatedAt();
            log.warn("TALENTOPOS 数据源配置不可用，已跳过自动恢复: {}", ex.getMessage());
        }
    }

    @Override
    public synchronized DataSourceStatusVO save(DataSourceDTO form) {
        requireForm(form);
        try {
            apply(form, true);
            saveConfig(form);
            log.info("TALENTOPOS 数据源已热更新: {}:{}/{}", form.getHost(), form.getPort(), form.getDatabaseName());
            return status();
        } catch (SQLException ex) {
            throw new BusinessException("保存数据源配置失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    public DataSourceStatusVO status() {
        if (config == null) {
            return DataSourceStatusVO.builder()
                    .configured(false)
                    .connected(false)
                    .message("尚未配置 TALENTOPOS 数据源")
                    .build();
        }
        boolean connected;
        String message;
        try {
            connected = ping();
            message = connected ? "连接正常" : "已配置但连接不可用";
        } catch (SQLException ex) {
            connected = false;
            message = "已配置但连接失败: " + ex.getMessage();
        } catch (BusinessException ex) {
            connected = false;
            message = ex.getMessage();
        }
        return DataSourceStatusVO.builder()
                .configured(true)
                .connected(connected)
                .host(config.getHost())
                .port(config.getPort())
                .databaseName(config.getDatabaseName())
                .username(config.getUsername())
                .lastAppliedAt(lastAppliedAt)
                .message(message)
                .build();
    }

    @Override
    public long countProducts() {
        try (var session = currentSqlSessionFactory().openSession()) {
            return session.getMapper(ProductMapper.class).selectCount(Wrappers.emptyWrapper());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("统计商品数量失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Supplier getSupplierById(String supplierGuid) {
        try (var session = currentSqlSessionFactory().openSession()) {
            if (StrUtil.isNotEmpty(supplierGuid)) {
                return session.getMapper(SupplierMapper.class).selectById(supplierGuid);
            }
        } catch (Exception e) {
            log.error("查询供应商失败, supplierGuid={}", supplierGuid, e);
        }
        return null;
    }

    @Override
    public List<Supplier> listSuppliers() {
        try (var session = currentSqlSessionFactory().openSession()) {
            return session.getMapper(SupplierMapper.class)
                    .selectList(Wrappers.lambdaQuery(Supplier.class)
                            .select(Supplier::getGuid, Supplier::getChineseName, Supplier::getForeignName)
                            .orderByAsc(Supplier::getChineseName));
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("查询供应商列表失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String validateSupplier(String supplierGuid) {
        if (StrUtil.isBlank(supplierGuid)) {
            throw new BusinessException("supplierGuid 不能为空");
        }
        supplierGuid = GuidHelper.normalize(supplierGuid);
        if (getSupplierById(supplierGuid) == null) {
            throw new BusinessException("供应商不存在: " + supplierGuid);
        }
        return supplierGuid;
    }

    private void requireForm(DataSourceDTO form) {
        if (form == null) {
            throw new BusinessException("数据源配置不能为空");
        }
    }

    private void apply(DataSourceDTO form, boolean updateAppliedTime) throws SQLException {
        HikariDataSource newDataSource = createDataSource(form);
        try (Connection ignored = newDataSource.getConnection()) {
            HikariDataSource oldDataSource = dataSource;
            dataSource = newDataSource;
            sqlSessionFactory = createSqlSessionFactory(newDataSource);
            config = form;
            if (updateAppliedTime) {
                lastAppliedAt = LocalDateTime.now();
            }
            closeQuietly(oldDataSource);
        } catch (SQLException ex) {
            closeQuietly(newDataSource);
            throw ex;
        }
    }

    private boolean ping() throws SQLException {
        try (Connection connection = currentDataSource().getConnection()) {
            return connection.isValid(3);
        }
    }

    private HikariDataSource currentDataSource() {
        HikariDataSource current = dataSource;
        if (current == null) {
            throw new BusinessException("TALENTOPOS 数据源尚未配置，请先在页面中保存连接信息");
        }
        return current;
    }

    private SqlSessionFactory currentSqlSessionFactory() {
        SqlSessionFactory current = sqlSessionFactory;
        if (current == null) {
            throw new BusinessException("TALENTOPOS 数据源尚未配置，请先在页面中保存连接信息");
        }
        return current;
    }

    private HikariDataSource createDataSource(DataSourceDTO form) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("TalentOposPool");
        hikariConfig.setDriverClassName(DRIVER_CLASS);
        hikariConfig.setJdbcUrl(form.jdbcUrl());
        hikariConfig.setUsername(form.getUsername());
        hikariConfig.setPassword(form.getPassword());
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setConnectionTestQuery("SELECT 1");
        return new HikariDataSource(hikariConfig);
    }

    private SqlSessionFactory createSqlSessionFactory(HikariDataSource dataSource) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setEnvironment(new Environment("talentopos", new JdbcTransactionFactory(), dataSource));
        configuration.addMapper(ProductMapper.class);
        configuration.addMapper(SupplierMapper.class);
        return new MybatisSqlSessionFactoryBuilder().build(configuration);
    }

    private void saveConfig(DataSourceDTO form) {
        TalentDataSourceConfig entity = TalentDataSourceConfig.builder()
                .id(CONFIG_ID)
                .host(form.getHost())
                .port(form.getPort())
                .databaseName(form.getDatabaseName())
                .username(form.getUsername())
                .password(TextEncryptor.encrypt(form.getPassword(), datasourceConfigKey))
                .updatedAt(lastAppliedAt)
                .build();
        if (talentDataSourceConfigMapper.selectById(CONFIG_ID) == null) {
            talentDataSourceConfigMapper.insert(entity);
            return;
        }
        talentDataSourceConfigMapper.updateById(entity);
    }

    private DataSourceDTO toRequest(TalentDataSourceConfig configEntity) {
        DataSourceDTO form = new DataSourceDTO();
        form.setHost(configEntity.getHost());
        form.setPort(configEntity.getPort());
        form.setDatabaseName(configEntity.getDatabaseName());
        form.setUsername(configEntity.getUsername());
        form.setPassword(TextEncryptor.decrypt(configEntity.getPassword(), datasourceConfigKey));
        return form;
    }

    private void closeQuietly(HikariDataSource dataSource) {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
