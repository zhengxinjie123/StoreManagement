package com.joao.storemanagement.security;

import com.joao.storemanagement.dto.security.MenuItemDTO;

import java.util.List;
import java.util.Set;

/**
 * 前端菜单与权限映射目录。
 */
public final class MenuCatalog {

    private static final List<MenuItemDTO> ALL = List.of(
            new MenuItemDTO("dashboard", "首页", "/dashboard", SystemPermission.DASHBOARD_VIEW.name()),
            new MenuItemDTO("invoice", "电子发票", "/invoice", SystemPermission.INVOICE_READ.name()),
            new MenuItemDTO("emailInbox", "邮箱发票", "/invoice/email", SystemPermission.INVOICE_READ.name()),
            new MenuItemDTO("invoiceArchive", "发票归档", "/invoice/archive", SystemPermission.INVOICE_READ.name()),
            new MenuItemDTO("invoiceTemplate", "清洗模板", "/invoice/template", SystemPermission.INVOICE_READ.name()),
            new MenuItemDTO("finance", "财务管理", "/finance", SystemPermission.FINANCE_READ.name()),
            new MenuItemDTO("replenish", "补货登记", "/replenish", SystemPermission.REPLENISH_READ.name()),
            new MenuItemDTO("analytics", "进价分析", "/analytics", SystemPermission.ANALYTICS_READ.name()),
            new MenuItemDTO("pricing", "售价参考", "/pricing", SystemPermission.PRICING_READ.name()),
            new MenuItemDTO("retailPriceManage", "售价管理", "/pricing/manage", SystemPermission.PRICING_READ.name()),
            new MenuItemDTO("appConfig", "系统参数", "/system/config", SystemPermission.SYSTEM_MANAGE.name()),
            new MenuItemDTO("talentDataSource", "TALENTOPOS 数据源", "/system/talent-datasource", SystemPermission.SYSTEM_MANAGE.name()),
            new MenuItemDTO("users", "用户管理", "/system/users", SystemPermission.SYSTEM_MANAGE.name()),
            new MenuItemDTO("operationLog", "操作日志", "/system/logs", SystemPermission.SYSTEM_MANAGE.name()),
            new MenuItemDTO("health", "系统健康", "/system/health", SystemPermission.SYSTEM_MANAGE.name()),
            new MenuItemDTO("apiDocs", "接口文档", "/system/api-docs", SystemPermission.SYSTEM_MANAGE.name()));

    private MenuCatalog() {
    }

    public static List<MenuItemDTO> menusFor(Set<SystemPermission> permissions) {
        return ALL.stream()
                .filter(menu -> permissions.contains(SystemPermission.valueOf(menu.permission())))
                .toList();
    }
}
