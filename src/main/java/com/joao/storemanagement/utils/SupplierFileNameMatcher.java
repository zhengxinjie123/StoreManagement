package com.joao.storemanagement.utils;

import cn.hutool.core.util.StrUtil;
import com.joao.storemanagement.entity.talent.Supplier;
import com.joao.storemanagement.vo.primary.BatchUploadSupplierMatchVO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 根据附件文件名匹配供应商（文件名包含供应商中/外文名即视为匹配）。
 */
@Component
public class SupplierFileNameMatcher {

    /**
     * 在供应商列表中为文件名查找最佳匹配；多个命中时取名称最长者，避免短名误匹配。
     */
    public BatchUploadSupplierMatchVO match(String fileName, List<Supplier> suppliers) {
        if (StrUtil.isBlank(fileName) || suppliers == null || suppliers.isEmpty()) {
            return unmatched(fileName);
        }
        Supplier matched = null;
        int bestLength = 0;
        for (Supplier supplier : suppliers) {
            int matchLength = longestContainedNameLength(fileName, supplier);
            if (matchLength > bestLength) {
                bestLength = matchLength;
                matched = supplier;
            }
        }
        if (matched == null) {
            return unmatched(fileName);
        }
        return BatchUploadSupplierMatchVO.builder()
                .fileName(fileName)
                .supplierGuid(matched.getGuid())
                .supplierChineseName(matched.getChineseName())
                .supplierForeignName(matched.getForeignName())
                .build();
    }

    private int longestContainedNameLength(String fileName, Supplier supplier) {
        int maxLength = 0;
        maxLength = Math.max(maxLength, containedNameLength(fileName, supplier.getChineseName()));
        maxLength = Math.max(maxLength, containedNameLength(fileName, supplier.getForeignName()));
        return maxLength;
    }

    private int containedNameLength(String fileName, String supplierName) {
        if (StrUtil.isBlank(supplierName)) {
            return 0;
        }
        String trimmed = supplierName.trim();
        return StrUtil.containsIgnoreCase(fileName, trimmed) ? trimmed.length() : 0;
    }

    private BatchUploadSupplierMatchVO unmatched(String fileName) {
        return BatchUploadSupplierMatchVO.builder()
                .fileName(fileName)
                .build();
    }
}
