package com.joao.storemanagement.vo.pricing;

import com.joao.storemanagement.entity.pricing.RetailPriceSyncRunLine;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RetailPriceSyncRunLineVO {

    private final Long id;
    private final Long runId;
    private final String productGuid;
    private final String barcode;
    private final BigDecimal oldRetailPriceTax;
    private final BigDecimal newRetailPriceTax;
    private final String sourceCode;
    private final String sourceName;
    private final LocalDateTime createdAt;

    public static RetailPriceSyncRunLineVO of(RetailPriceSyncRunLine line) {
        return RetailPriceSyncRunLineVO.builder()
                .id(line.getId())
                .runId(line.getRunId())
                .productGuid(line.getProductGuid())
                .barcode(line.getBarcode())
                .oldRetailPriceTax(line.getOldRetailPriceTax())
                .newRetailPriceTax(line.getNewRetailPriceTax())
                .sourceCode(line.getSourceCode())
                .sourceName(line.getSourceName())
                .createdAt(line.getCreatedAt())
                .build();
    }
}
