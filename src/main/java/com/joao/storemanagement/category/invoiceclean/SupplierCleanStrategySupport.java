package com.joao.storemanagement.category.invoiceclean;

import cn.hutool.core.util.StrUtil;
import com.joao.storemanagement.entity.talent.Supplier;
import com.joao.storemanagement.service.talent.TalentOposDataSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupplierCleanStrategySupport {

    private final TalentOposDataSourceService talentOposDataSourceService;

    public boolean matchesSupplierName(String supplierGuid, String keyword) {
        if (StrUtil.isBlank(supplierGuid) || StrUtil.isBlank(keyword)) {
            return false;
        }
        Supplier supplier = findSupplierQuietly(supplierGuid);
        if (supplier == null) {
            return false;
        }
        return StrUtil.containsIgnoreCase(supplier.getChineseName(), keyword)
                || StrUtil.containsIgnoreCase(supplier.getForeignName(), keyword);
    }

    private Supplier findSupplierQuietly(String supplierGuid) {
        try {
            return talentOposDataSourceService.getSupplierById(supplierGuid);
        } catch (Exception ex) {
            return null;
        }
    }
}
