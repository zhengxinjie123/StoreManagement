package com.joao.storemanagement.entity.primary;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("invoice_archives")
public class InvoiceArchive {

    @TableId(value = "uuid", type = IdType.ASSIGN_UUID)
    private String uuid;

    @TableField("attachment_uuid")
    private String attachmentUuid;

    @TableField("supplier_guid")
    private String supplierGuid;

    @TableField("file_name")
    private String fileName;

    @TableField("extension_name")
    private String extensionName;

    @TableField("file_size")
    private Long fileSize;

    @TableField("file_path")
    private String filePath;

    @TableField("row_count")
    private Integer rowCount;

    @TableField("total_quantity")
    private BigDecimal totalQuantity;

    @TableField("amount_before_discount")
    private BigDecimal amountBeforeDiscount;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("tax_included")
    private Boolean taxIncluded;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
