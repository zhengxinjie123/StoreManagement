package com.joao.storemanagement.talent.purchase.dto;

import cn.hutool.core.util.StrUtil;
import com.joao.storemanagement.config.StoreProperties;
import com.joao.storemanagement.talent.purchase.entity.Product;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductImportDTO {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final String EMPTY = "";
    public static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("0.23");

    private String barcode;
    private String name;
    private String nameCn;
    private Integer quantity;
    private BigDecimal price;
    private Boolean priceIncludesTax;
    private BigDecimal taxRate;

    public BigDecimal effectiveTaxRate() {
        return taxRate != null ? taxRate : DEFAULT_TAX_RATE;
    }

    public Product toNewProduct(
            String guid,
            String productCode,
            StoreProperties.ProductDefaults defaults,
            String supplierGuid,
            LocalDateTime now) {
        Product product = new Product();
        product.setGuid(guid);
        product.setTypeGuid(defaults.getTypeGuid());
        product.setProductUnitGuid(defaults.getProductUnitGuid());
        product.setDefaultSupplierGuid(supplierGuid);
        product.setDefaultDepotGuid(defaults.getDepotGuid());
        product.setLabelStyleGuid(defaults.getLabelStyleGuid());
        product.setProductLabelStyleGuid(defaults.getProductLabelStyleGuid());
        product.setNo(productCode);
        product.setBarcode(StrUtil.nullToEmpty(barcode));
        product.setName(StrUtil.nullToEmpty(nameCn));
        product.setComposition(EMPTY);
        product.setCombinable(false);
        product.setRetailPrice(ZERO);
        product.setRetailPriceTax(ZERO);
        product.setTaxRate(effectiveTaxRate());
        product.setSafetyStock(ZERO);
        product.setPackageQuantity(ONE);
        product.setDiscountRate(ONE);
        product.setFixedDiscountRate(false);
        product.setImage(EMPTY);
        product.setUpdateDate(now);
        product.setSpecialPrice(ZERO);
        product.setSpecialPriceTax(ZERO);
        product.setWholesalePrice(ZERO);
        product.setWholesalePriceTax(ZERO);
        product.setVipPrice(ZERO);
        product.setVipPriceTax(ZERO);
        product.setMemberPrice(ZERO);
        product.setMemberPriceTax(ZERO);
        product.setCustom1(EMPTY);
        product.setCustom2(EMPTY);
        product.setCustom3(EMPTY);
        product.setCustom4(EMPTY);
        product.setRemark(EMPTY);
        product.setNameP(StrUtil.nullToEmpty(name));
        product.setDeliverPrice(ZERO);
        product.setDeliverPriceTax(ZERO);
        product.setOnlinePrice(ZERO);
        product.setOnlinePriceTax(ZERO);
        product.setFriendlyPrice(ZERO);
        product.setFriendlyPriceTax(ZERO);
        product.setDisabled(false);
        product.setEnableAutoPrice(false);
        product.setEnableAutoDiscountRate(false);
        product.setAutoQuantityLevel1(0);
        product.setAutoQuantityLevel2(0);
        product.setAutoQuantityLevel3(0);
        product.setAutoQuantityLevel4(0);
        product.setAutoQuantityLevel5(0);
        product.setAutoPriceLevel0(ZERO);
        product.setAutoPriceLevel1(ZERO);
        product.setAutoPriceLevel2(ZERO);
        product.setAutoPriceLevel3(ZERO);
        product.setAutoPriceLevel4(ZERO);
        product.setAutoPriceLevel5(ZERO);
        product.setAutoDiscountRateLevel0(ONE);
        product.setAutoDiscountRateLevel1(ONE);
        product.setAutoDiscountRateLevel2(ONE);
        product.setAutoDiscountRateLevel3(ONE);
        product.setAutoDiscountRateLevel4(ONE);
        product.setAutoDiscountRateLevel5(ONE);
        product.setVolume(ZERO);
        product.setModel(EMPTY);
        product.setSpec(EMPTY);
        product.setBrand(EMPTY);
        product.setQualityStandard(EMPTY);
        product.setPackaging(EMPTY);
        product.setManual(EMPTY);
        product.setValidity(ZERO);
        product.setEnWeightingScale(false);
        product.setEcoReeeTaxRate(ZERO);
        product.setEcoImp(ZERO);
        product.setEcoReeeImp(ZERO);
        product.setEcoTax(-1);
        product.setUpdateTime(now);
        product.setDisableDiscount(false);
        product.setAccQuantity(ZERO);
        product.setQuantityBk(ZERO);
        product.setInvoiceQuantityBk(ZERO);
        product.setUsefulLife(now);
        product.setHasExpireDate(false);
        product.setCloudId(null);
        product.setSyn(true);
        product.setAverageDailySaleQuantity(ZERO);
        product.setFussySearchKeyWord(
                StrUtil.nullToEmpty(productCode) + " "
                        + StrUtil.nullToEmpty(barcode) + " "
                        + StrUtil.nullToEmpty(nameCn) + " "
                        + StrUtil.nullToEmpty(name));

        applyPurchasePrices(product);
        return product;
    }

    private void applyPurchasePrices(Product product) {
        BigDecimal taxRate = effectiveTaxRate();
        if (price == null) {
            product.setPurchasePrice(ZERO);
            product.setPurchasePriceTax(ZERO);
            return;
        }
        if (Boolean.TRUE.equals(priceIncludesTax)) {
            product.setPurchasePriceTax(price);
            product.setPurchasePrice(price.subtract(price.multiply(taxRate)));
        } else {
            product.setPurchasePrice(price);
            product.setPurchasePriceTax(price.add(price.multiply(taxRate)));
        }
    }
}
