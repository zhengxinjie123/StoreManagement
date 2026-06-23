package com.joao.storemanagement.category.invoiceclean;

/**
 * 发票清洗策略 Spring Bean 名称常量。
 * Bean 名称与供应商名称匹配关键字一致，供 {@link InvoiceCleanStrategyFactory} 通过 Map 注入解析。
 */
public final class InvoiceCleanStrategyKeys {

    public static final String DEFAULT = "default";
    public static final String HONGTAIYANG = "红太阳";
    public static final String MAXI = "MAXI";
    public static final String CHENGUANG = "晨光";
    public static final String DAZHONG = "大众";
    public static final String FEIYUE = "飞跃";
    public static final String HAOPENGYOU = "好朋友";
    public static final String JINDONG = "金东";
    public static final String AIGUOZHE = "爱国者";

    private InvoiceCleanStrategyKeys() {
    }
}
