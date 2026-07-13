package com.joao.storemanagement.dto.primary;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 预览确认归档入参：携带前端编辑后的明细行与汇总。
 */
@Getter
@Setter
public class InvoiceCleanConfirmDTO {

    @NotBlank(message = "supplierGuid 不能为空")
    private String supplierGuid;

    @Valid
    @NotEmpty(message = "归档明细不能为空")
    private List<Row> rows;

    @Valid
    private Summary summary;

    @Getter
    @Setter
    public static class Row {
        private String barcode;
        private String foreignName;
        private BigDecimal quantity;
        private BigDecimal outputPrice;
        private BigDecimal taxRate;
    }

    @Getter
    @Setter
    public static class Summary {
        private BigDecimal totalQuantity;
        private BigDecimal amountBeforeDiscount;
        private BigDecimal discountAmount;
        private BigDecimal totalAmount;
        private Boolean taxIncluded;
        private Integer filteredCount;
        private BigDecimal filteredAmount;
        private String remark;
    }
}
