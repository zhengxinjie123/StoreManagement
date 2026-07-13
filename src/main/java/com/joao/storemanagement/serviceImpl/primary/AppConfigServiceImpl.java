package com.joao.storemanagement.serviceImpl.primary;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.joao.storemanagement.dto.primary.AppConfigDTO;
import com.joao.storemanagement.entity.primary.AppConfig;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.mapper.primary.AppConfigMapper;
import com.joao.storemanagement.service.primary.AppConfigService;
import com.joao.storemanagement.vo.primary.AppConfigVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppConfigServiceImpl implements AppConfigService {

    private final AppConfigMapper appConfigMapper;

    @PostConstruct
    public void seedDefaults() {
        reseedDefaults();
    }

    @Override
    public void reseedDefaults() {
        try {
            for (DefaultGroup group : defaultGroups()) {
                AppConfig groupEntity = ensureGroup(group);
                for (DefaultItem item : group.items()) {
                    ensureItem(groupEntity.getId(), item);
                }
            }
        } catch (RuntimeException ex) {
            log.warn("初始化 app_config 默认配置失败，继续使用代码默认值: {}", ex.getMessage());
        }
    }

    @Override
    public List<AppConfigVO> tree() {
        List<AppConfig> configs = appConfigMapper.selectList(Wrappers.lambdaQuery(AppConfig.class)
                .orderByAsc(AppConfig::getParentId)
                .orderByAsc(AppConfig::getSortOrder)
                .orderByAsc(AppConfig::getId));
        Map<Long, List<AppConfig>> byParent = new LinkedHashMap<>();
        for (AppConfig config : configs) {
            byParent.computeIfAbsent(config.getParentId(), key -> new ArrayList<>()).add(config);
        }
        return buildTree(null, byParent);
    }

    @Override
    public AppConfigVO create(AppConfigDTO form) {
        requireForm(form);
        if (findByKey(form.getConfigKey()) != null) {
            throw new BusinessException("配置 key 已存在: " + form.getConfigKey());
        }
        LocalDateTime now = LocalDateTime.now();
        AppConfig entity = toEntity(form);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        appConfigMapper.insert(entity);
        return AppConfigVO.of(entity, List.of());
    }

    @Override
    public AppConfigVO update(Long id, AppConfigDTO form) {
        if (id == null) {
            throw new BusinessException("配置 ID 不能为空");
        }
        requireForm(form);
        AppConfig entity = appConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("配置不存在: " + id);
        }
        AppConfig sameKey = findByKey(form.getConfigKey());
        if (sameKey != null && !Objects.equals(sameKey.getId(), id)) {
            throw new BusinessException("配置 key 已存在: " + form.getConfigKey());
        }
        applyForm(entity, form);
        entity.setUpdatedAt(LocalDateTime.now());
        appConfigMapper.updateById(entity);
        return AppConfigVO.of(entity, List.of());
    }

    @Override
    public void delete(Long id) {
        if (id == null) {
            throw new BusinessException("配置 ID 不能为空");
        }
        Long childCount = appConfigMapper.selectCount(Wrappers.lambdaQuery(AppConfig.class)
                .eq(AppConfig::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException("请先删除子配置");
        }
        if (appConfigMapper.deleteById(id) == 0) {
            throw new BusinessException("配置不存在: " + id);
        }
    }

    @Override
    public String getString(String key, String defaultValue) {
        try {
            AppConfig config = findByKey(key);
            return config == null || StrUtil.isBlank(config.getConfigValue())
                    ? defaultValue
                    : config.getConfigValue();
        } catch (RuntimeException ex) {
            return defaultValue;
        }
    }

    @Override
    public Boolean getBoolean(String key, Boolean defaultValue) {
        String value = getString(key, null);
        if (StrUtil.isBlank(value)) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "是".equals(value);
    }

    @Override
    public Integer getInteger(String key, Integer defaultValue) {
        String value = getString(key, null);
        if (StrUtil.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    @Override
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        String value = getString(key, null);
        if (StrUtil.isBlank(value)) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private AppConfig ensureGroup(DefaultGroup group) {
        AppConfig existing = findByKey(group.key());
        if (existing != null) {
            return existing;
        }
        LocalDateTime now = LocalDateTime.now();
        AppConfig entity = AppConfig.builder()
                .parentId(null)
                .configKey(group.key())
                .configValue(null)
                .valueType("GROUP")
                .label(group.label())
                .description(group.description())
                .sortOrder(group.sortOrder())
                .systemFlag(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
        appConfigMapper.insert(entity);
        return entity;
    }

    private void ensureItem(Long parentId, DefaultItem item) {
        if (findByKey(item.key()) != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        appConfigMapper.insert(AppConfig.builder()
                .parentId(parentId)
                .configKey(item.key())
                .configValue(item.value())
                .valueType(item.type())
                .label(item.label())
                .description(item.description())
                .sortOrder(item.sortOrder())
                .systemFlag(true)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private AppConfig findByKey(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        return appConfigMapper.selectOne(Wrappers.lambdaQuery(AppConfig.class)
                .eq(AppConfig::getConfigKey, key.trim())
                .orderByAsc(AppConfig::getId)
                .last("OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY"));
    }

    private List<AppConfigVO> buildTree(Long parentId, Map<Long, List<AppConfig>> byParent) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .sorted(Comparator.comparing(
                        AppConfig::getSortOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(item -> AppConfigVO.of(item, buildTree(item.getId(), byParent)))
                .toList();
    }

    private void requireForm(AppConfigDTO form) {
        if (form == null) {
            throw new BusinessException("配置参数不能为空");
        }
        if (StrUtil.isBlank(form.getConfigKey())) {
            throw new BusinessException("配置 key 不能为空");
        }
        if (StrUtil.isBlank(form.getValueType())) {
            throw new BusinessException("值类型不能为空");
        }
        if (StrUtil.isBlank(form.getLabel())) {
            throw new BusinessException("配置名称不能为空");
        }
    }

    private AppConfig toEntity(AppConfigDTO form) {
        AppConfig entity = new AppConfig();
        applyForm(entity, form);
        return entity;
    }

    private void applyForm(AppConfig entity, AppConfigDTO form) {
        entity.setParentId(form.getParentId());
        entity.setConfigKey(form.getConfigKey().trim());
        entity.setConfigValue(StrUtil.blankToDefault(form.getConfigValue(), null));
        entity.setValueType(form.getValueType().trim().toUpperCase());
        entity.setLabel(form.getLabel().trim());
        entity.setDescription(StrUtil.blankToDefault(form.getDescription(), null));
        entity.setSortOrder(form.getSortOrder() == null ? 0 : form.getSortOrder());
        entity.setSystemFlag(Boolean.TRUE.equals(form.getSystemFlag()));
    }

    private static List<DefaultGroup> defaultGroups() {
        return List.of(
                new DefaultGroup("security", "安全配置", "系统安全与加密相关配置", 10, List.of(
                        item("security.datasource-config-key", "store-management-local-key", "PASSWORD", "数据源配置加密 Key", 10)
                )),
                new DefaultGroup("google-drive", "Google Drive 配置", "父母发票上传云端配置", 20, List.of(
                        item("google-drive.enabled", "true", "BOOLEAN", "是否启用", 10),
                        item("google-drive.client-id", "", "STRING", "Client ID", 20),
                        item("google-drive.client-secret", "", "PASSWORD", "Client Secret", 30),
                        item("google-drive.token-path", "C:/Users/z1286/Desktop/Scan-delivery/token.json", "STRING", "Token 路径", 40),
                        item("google-drive.folder-name", "Fatura", "STRING", "文件夹名称", 50),
                        item("google-drive.folder-id", "", "STRING", "文件夹 ID", 60)
                )),
                new DefaultGroup("upload", "上传路径配置", "附件与归档文件的本地存储路径", 30, List.of(
                        item("upload.attachment-dir", "uploads/import-attachments", "STRING", "附件目录", 10),
                        item("upload.invoice-archive-dir", "uploads/invoice-archives", "STRING", "归档目录", 20)
                )),
                new DefaultGroup("invoice", "发票配置", "发票清洗默认配置", 40, List.of(
                        item("invoice.default-tax-rate", "23", "DECIMAL", "默认税率", 10)
                )),
                new DefaultGroup("archive", "归档配置", "归档文件命名配置", 50, List.of(
                        item("archive.max-name-suffix-attempts", "1000", "INTEGER", "最大重名尝试次数", 10)
                )),
                new DefaultGroup("product", "TALENT 商品默认值", "采购导入新商品默认 GUID", 60, List.of(
                        item("product.type-guid", "57c8854a-b1d2-4fa0-b5a0-057b547a3e97", "STRING", "商品类型 GUID", 10),
                        item("product.supplier-guid", "4cd95e7a-a422-4334-a82f-678ebf6f5363", "STRING", "默认供应商 GUID", 20),
                        item("product.product-unit-guid", "f4bf65c4-18d9-49d3-ba03-734d1500e80d", "STRING", "商品单位 GUID", 30),
                        item("product.depot-guid", "c3308645-2dd7-4ddd-8596-3bf0cb63c88f", "STRING", "仓库 GUID", 40),
                        item("product.label-style-guid", "6E7985B0-5A58-4411-A3AB-4F867E201DC9", "STRING", "标签样式 GUID", 50),
                        item("product.product-label-style-guid", "DF53D3B7-19C6-467D-8A10-64749525A047", "STRING", "商品标签样式 GUID", 60),
                        item("product.batch-no", "SimpleBatch", "STRING", "批次号", 70)
                )),
                new DefaultGroup("purchase", "TALENT 采购默认值", "采购入库默认用户与供应商类型", 70, List.of(
                        item("purchase.employee-guid", "ad4a220a-1e7a-4359-b680-6e036f56d811", "STRING", "员工 GUID", 10),
                        item("purchase.marker-user-guid", "5dbf3933-1cf4-4d3c-bef0-27b3b56f9697", "STRING", "制单人 GUID", 20),
                        item("purchase.approver-user-guid", "5dbf3933-1cf4-4d3c-bef0-27b3b56f9697", "STRING", "审批人 GUID", 30),
                        item("purchase.product-unit-name", "p", "STRING", "商品单位名称", 40),
                        item("purchase.chinese-supplier-type-guid", "EA924E2E-7936-4D8C-841E-0F7E9A445DE3", "STRING", "中文供应商类型 GUID", 50),
                        item("purchase.foreign-supplier-type-guid", "25AFFF2D-A59A-4582-B98E-BE545DB916A5", "STRING", "外文供应商类型 GUID", 60),
                        item("purchase.chinese-supplier-iso-country-code", "CN", "STRING", "中文供应商国家", 70),
                        item("purchase.foreign-supplier-iso-country-code", "PT", "STRING", "外文供应商国家", 80)
                )),
                new DefaultGroup("email", "邮箱配置", "从邮箱读取电子发票附件", 80, List.of(
                        item("email.enabled", "false", "BOOLEAN", "是否启用", 10),
                        item("email.host", "", "STRING", "IMAP 服务器", 20),
                        item("email.port", "993", "INTEGER", "IMAP 端口", 30),
                        item("email.username", "", "STRING", "邮箱账号", 40),
                        item("email.password", "", "PASSWORD", "邮箱密码/授权码", 50),
                        item("email.use-ssl", "true", "BOOLEAN", "启用 SSL", 60),
                        item("email.allowed-extensions", "xls,xlsx,pdf", "STRING", "允许附件扩展名", 70),
                        item("email.storage-dir", "uploads/email-inbox", "STRING", "邮箱附件缓存目录", 80)
                ))
        );
    }

    private static DefaultItem item(String key, String value, String type, String label, int sortOrder) {
        return new DefaultItem(key, value, type, label, null, sortOrder);
    }

    private record DefaultGroup(
            String key, String label, String description, int sortOrder, List<DefaultItem> items) {
    }

    private record DefaultItem(
            String key, String value, String type, String label, String description, int sortOrder) {
    }
}
