package com.joao.storemanagement.system;

import java.util.Set;

public final class DatabaseResetConstants {

    /**
     * 允许被重置的主库名称（仅 Store_Management，不影响 TALENTOPOS 外部库）。
     */
    public static final String TARGET_DATABASE = "Store_Management";

    /**
     * 数据库重置二次确认口令（写死在代码中，仅用于高危迁移场景）。
     */
    public static final String CONFIRM_PASSWORD = "StoreReset@2026";

    /**
     * 重置时保留数据的表（系统参数等配置不应随业务数据清空）。
     */
    public static final Set<String> PROTECTED_TABLES = Set.of("app_config");

    private DatabaseResetConstants() {
    }
}
