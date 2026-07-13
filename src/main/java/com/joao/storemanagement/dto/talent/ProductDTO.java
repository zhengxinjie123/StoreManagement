package com.joao.storemanagement.dto.talent;

import java.math.BigDecimal;

/**
 * 商品响应对象
 *
 * @param guid             主键
 * @param productCode      产品编码
 * @param barcode          产品条形码
 * @param supplierName     供应商名称
 * @param nameForeign      产品外文名
 * @param nameChinese      产品中文名
 * @param quantity         数量
 * @param purchasePrice    不含税进价
 * @param purchasePriceTax 含税进价
 * @param taxRate          税率
 * @param retailPrice      零售价含税
 */
public record ProductDTO(
        String guid,
        String productCode,
        String barcode,
        String supplierName,
        String nameForeign,
        String nameChinese,
        BigDecimal quantity,
        BigDecimal purchasePrice,
        BigDecimal purchasePriceTax,
        BigDecimal taxRate,
        BigDecimal retailPrice) {
}


