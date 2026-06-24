package com.joao.storemanagement.category.invoiceclean;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 发票清洗策略工厂。通过 Spring 注入 {@code Map<String, InvoiceCleanStrategy>}，
 * 以 Bean 名称（供应商匹配关键字）解析对应策略，未命中时回退至默认策略。
 */
@Component
public class InvoiceCleanStrategyFactory {

    private final InvoiceCleanStrategy defaultStrategy;

    public InvoiceCleanStrategyFactory(Map<String, InvoiceCleanStrategy> strategyMap) {
        // 取出默认策略 Bean
        this.defaultStrategy = strategyMap.get(InvoiceCleanStrategyKeys.DEFAULT);
        if (defaultStrategy == null) {
            throw new IllegalStateException("缺少默认发票清洗策略 Bean: " + InvoiceCleanStrategyKeys.DEFAULT);
        }
    }

    /**
     * 根据供应商 GUID 解析匹配的清洗策略。
     * 当前供应商差异已迁移到模板热配置字段，统一返回默认策略；保留工厂作为后续特殊策略扩展点。
     */
    public InvoiceCleanStrategy resolve(String supplierGuid) {
        return defaultStrategy;
    }
}
