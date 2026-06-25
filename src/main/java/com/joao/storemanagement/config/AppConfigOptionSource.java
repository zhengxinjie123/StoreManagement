package com.joao.storemanagement.config;

import java.util.Map;

/**
 * 系统参数与 TALENT 参考数据源的映射。
 */
public final class AppConfigOptionSource {

    public static final String PRODUCT_TYPE = "PRODUCT_TYPE";
    public static final String SUPPLIER = "SUPPLIER";
    public static final String PRODUCT_UNIT = "PRODUCT_UNIT";
    public static final String PRODUCT_UNIT_NAME = "PRODUCT_UNIT_NAME";
    public static final String DEPOT = "DEPOT";
    public static final String LABEL_STYLE = "LABEL_STYLE";
    public static final String PRODUCT_LABEL_STYLE = "PRODUCT_LABEL_STYLE";
    public static final String EMPLOYEE = "EMPLOYEE";
    public static final String USER = "USER";
    public static final String SUPPLIER_TYPE = "SUPPLIER_TYPE";

    private static final Map<String, String> BY_CONFIG_KEY = Map.ofEntries(
            Map.entry("product.type-guid", PRODUCT_TYPE),
            Map.entry("product.supplier-guid", SUPPLIER),
            Map.entry("product.product-unit-guid", PRODUCT_UNIT),
            Map.entry("product.depot-guid", DEPOT),
            Map.entry("product.label-style-guid", LABEL_STYLE),
            Map.entry("product.product-label-style-guid", PRODUCT_LABEL_STYLE),
            Map.entry("purchase.employee-guid", EMPLOYEE),
            Map.entry("purchase.marker-user-guid", USER),
            Map.entry("purchase.approver-user-guid", USER),
            Map.entry("purchase.product-unit-name", PRODUCT_UNIT_NAME),
            Map.entry("purchase.chinese-supplier-type-guid", SUPPLIER_TYPE),
            Map.entry("purchase.foreign-supplier-type-guid", SUPPLIER_TYPE));

    private AppConfigOptionSource() {
    }

    public static String resolve(String configKey) {
        if (configKey == null) {
            return null;
        }
        return BY_CONFIG_KEY.get(configKey.trim());
    }
}
