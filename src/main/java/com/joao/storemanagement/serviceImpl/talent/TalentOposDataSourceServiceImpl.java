package com.joao.storemanagement.serviceImpl.talent;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.joao.storemanagement.dto.talent.DataSourceDTO;
import com.joao.storemanagement.entity.primary.TalentDataSourceConfig;
import com.joao.storemanagement.entity.talent.Supplier;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.mapper.primary.TalentDataSourceConfigMapper;
import com.joao.storemanagement.config.AppConfigOptionSource;
import com.joao.storemanagement.mapper.talent.ProductMapper;
import com.joao.storemanagement.mapper.talent.SupplierMapper;
import com.joao.storemanagement.mapper.talent.TalentReferenceMapper;
import com.joao.storemanagement.utils.TextEncryptorUtil;
import com.joao.storemanagement.service.primary.AppConfigService;
import com.joao.storemanagement.service.talent.TalentOposDataSourceService;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import com.joao.storemanagement.utils.GuidHelper;
import com.joao.storemanagement.vo.talent.DataSourceStatusVO;
import com.joao.storemanagement.vo.talent.TalentReferenceOptionVO;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class TalentOposDataSourceServiceImpl implements TalentOposDataSourceService {

    private static final long CONFIG_ID = 1L;
    private static final String DRIVER_CLASS = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    private final TalentDataSourceConfigMapper talentDataSourceConfigMapper;
    private final AppConfigService appConfigService;

    private volatile HikariDataSource dataSource;
    private volatile SqlSessionFactory sqlSessionFactory;
    private volatile DataSourceDTO config;
    private volatile LocalDateTime lastAppliedAt;

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

    @PreDestroy
    public void destroy() {
        closeQuietly(dataSource);
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
    public List<TalentReferenceOptionVO> listReferenceOptions(String type) {
        if (StrUtil.isBlank(type)) {
            throw new BusinessException("参考数据类型不能为空");
        }
        String optionType = type.trim();
        if (AppConfigOptionSource.SUPPLIER.equals(optionType)) {
            return listSupplierReferenceOptions();
        }
        try (var session = currentSqlSessionFactory().openSession()) {
            return queryReferenceOptions(session.getMapper(TalentReferenceMapper.class), optionType);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("查询 TALENT 参考数据失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Map<String, List<TalentReferenceOptionVO>> listReferenceOptionMap() {
        Map<String, List<TalentReferenceOptionVO>> result = new LinkedHashMap<>();
        try (var session = currentSqlSessionFactory().openSession()) {
            TalentReferenceMapper mapper = session.getMapper(TalentReferenceMapper.class);
            putReferenceOptions(result, AppConfigOptionSource.PRODUCT_TYPE, mapper);
            putReferenceOptions(result, AppConfigOptionSource.PRODUCT_UNIT, mapper);
            putReferenceOptions(result, AppConfigOptionSource.PRODUCT_UNIT_NAME, mapper);
            putReferenceOptions(result, AppConfigOptionSource.DEPOT, mapper);
            putReferenceOptions(result, AppConfigOptionSource.LABEL_STYLE, mapper);
            putReferenceOptions(result, AppConfigOptionSource.PRODUCT_LABEL_STYLE, mapper);
            putReferenceOptions(result, AppConfigOptionSource.EMPLOYEE, mapper);
            putReferenceOptions(result, AppConfigOptionSource.USER, mapper);
            putReferenceOptions(result, AppConfigOptionSource.SUPPLIER_TYPE, mapper);
        } catch (BusinessException ex) {
            log.warn("加载 TALENT 参考数据失败: {}", ex.getMessage());
        } catch (Exception ex) {
            log.warn("加载 TALENT 参考数据失败", ex);
        }
        putReferenceOptions(result, AppConfigOptionSource.SUPPLIER, this::listSupplierReferenceOptions);
        return result;
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

    @Override
    public <T> T executeInSession(Function<SqlSession, T> action) {
        try (SqlSession session = currentSqlSessionFactory().openSession()) {
            return action.apply(session);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("TALENTOPOS 操作失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    public <T> T executeInWriteTransaction(Function<SqlSession, T> action) {
        try (SqlSession session = currentSqlSessionFactory().openSession(false)) {
            try {
                T result = action.apply(session);
                session.commit();
                return result;
            } catch (RuntimeException ex) {
                session.rollback();
                throw ex;
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("TALENTOPOS 写操作失败: " + ex.getMessage(), ex);
        }
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
        try {
            MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
            factoryBean.setDataSource(dataSource);
            factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                    .getResources("classpath:mapper/talent/**/*.xml"));

            MybatisConfiguration configuration = new MybatisConfiguration();
            configuration.setMapUnderscoreToCamelCase(true);
            registerMappers(configuration);
            factoryBean.setConfiguration(configuration);
            factoryBean.afterPropertiesSet();
            return factoryBean.getObject();
        } catch (Exception ex) {
            throw new IllegalStateException("创建 TALENTOPOS SqlSessionFactory 失败", ex);
        }
    }

    private List<TalentReferenceOptionVO> listSupplierReferenceOptions() {
        return listSuppliers().stream()
                .map(supplier -> TalentReferenceOptionVO.builder()
                        .value(supplier.getGuid())
                        .code(supplier.getGuid())
                        .label(buildSupplierLabel(supplier))
                        .build())
                .toList();
    }

    private void putReferenceOptions(
            Map<String, List<TalentReferenceOptionVO>> result,
            String type,
            java.util.function.Supplier<List<TalentReferenceOptionVO>> loader) {
        try {
            result.put(type, loader.get());
        } catch (BusinessException ex) {
            log.warn("加载 TALENT 参考数据 {} 失败: {}", type, ex.getMessage());
            result.put(type, List.of());
        } catch (Exception ex) {
            log.warn("加载 TALENT 参考数据 {} 失败", type, ex);
            result.put(type, List.of());
        }
    }

    private void putReferenceOptions(
            Map<String, List<TalentReferenceOptionVO>> result,
            String type,
            TalentReferenceMapper mapper) {
        try {
            result.put(type, queryReferenceOptions(mapper, type));
        } catch (BusinessException ex) {
            log.warn("加载 TALENT 参考数据 {} 失败: {}", type, ex.getMessage());
            result.put(type, List.of());
        } catch (Exception ex) {
            log.warn("加载 TALENT 参考数据 {} 失败", type, ex);
            result.put(type, List.of());
        }
    }

    private static String buildSupplierLabel(Supplier supplier) {
        String chinese = StrUtil.trimToEmpty(supplier.getChineseName());
        String foreign = StrUtil.trimToEmpty(supplier.getForeignName());
        if (StrUtil.isNotBlank(chinese) && StrUtil.isNotBlank(foreign)) {
            return chinese + " / " + foreign;
        }
        if (StrUtil.isNotBlank(chinese)) {
            return chinese;
        }
        if (StrUtil.isNotBlank(foreign)) {
            return foreign;
        }
        return supplier.getGuid();
    }

    private static List<TalentReferenceOptionVO> queryReferenceOptions(TalentReferenceMapper mapper, String type) {
        return switch (type) {
            case AppConfigOptionSource.PRODUCT_TYPE -> mapper.listProductTypes();
            case AppConfigOptionSource.PRODUCT_UNIT -> mapper.listProductUnits();
            case AppConfigOptionSource.PRODUCT_UNIT_NAME -> mapper.listProductUnitNames();
            case AppConfigOptionSource.DEPOT -> mapper.listDepots();
            case AppConfigOptionSource.LABEL_STYLE -> mapper.listLabelStyles();
            case AppConfigOptionSource.PRODUCT_LABEL_STYLE -> mapper.listProductLabelStyles();
            case AppConfigOptionSource.EMPLOYEE -> mapper.listEmployees();
            case AppConfigOptionSource.USER -> mapper.listUsers();
            case AppConfigOptionSource.SUPPLIER_TYPE -> mapper.listSupplierTypes();
            default -> throw new BusinessException("不支持的参考数据类型: " + type);
        };
    }

    private static void registerMappers(MybatisConfiguration configuration) {
        registerMapperPackage(configuration, "com.joao.storemanagement.mapper.talent");
        registerMapperPackage(configuration, "com.joao.storemanagement.talent.purchase.mapper");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerMapperPackage(MybatisConfiguration configuration, String packageName) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resolver);
            String packagePath = ClassUtils.convertClassNameToResourcePath(packageName);
            Resource[] resources = resolver.getResources("classpath*:" + packagePath + "/**/*.class");
            for (Resource resource : resources) {
                String className = metadataReaderFactory.getMetadataReader(resource)
                        .getClassMetadata()
                        .getClassName();
                Class mapperClass = ClassUtils.forName(className, TalentOposDataSourceServiceImpl.class.getClassLoader());
                if (mapperClass.isInterface() && !configuration.hasMapper(mapperClass)) {
                    configuration.addMapper(mapperClass);
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("注册 TALENTOPOS Mapper 失败: " + packageName, ex);
        }
    }

    private void saveConfig(DataSourceDTO form) {
        TalentDataSourceConfig entity = TalentDataSourceConfig.builder()
                .id(CONFIG_ID)
                .host(form.getHost())
                .port(form.getPort())
                .databaseName(form.getDatabaseName())
                .username(form.getUsername())
                .password(TextEncryptorUtil.encrypt(form.getPassword(), datasourceConfigKey()))
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
        form.setPassword(TextEncryptorUtil.decrypt(configEntity.getPassword(), datasourceConfigKey()));
        return form;
    }

    private String datasourceConfigKey() {
        return appConfigService.getString("security.datasource-config-key", "store-management-local-key");
    }

    private void closeQuietly(HikariDataSource dataSource) {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
