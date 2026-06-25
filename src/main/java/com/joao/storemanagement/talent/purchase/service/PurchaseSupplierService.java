package com.joao.storemanagement.talent.purchase.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.joao.storemanagement.config.StoreProperties;
import com.joao.storemanagement.talent.purchase.entity.Supplier;
import com.joao.storemanagement.talent.purchase.mapper.PurchaseParameterMapper;
import com.joao.storemanagement.talent.purchase.mapper.PurchaseSupplierMapper;
import com.joao.storemanagement.talent.purchase.mapper.PurchaseSupplierTypeMapper;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
public class PurchaseSupplierService {

    private static final String PARAM_AUTO_CODE_LENGTH = "AutoCodeLength_Supplier";
    private static final String PARAM_WITH_TYPE_PREFIX = "AutoCodeWithTypeCodePrefix_Supplier";
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final String CHINESE_SUPPLIER_TYPE_NO = "04";
    private static final String FOREIGN_SUPPLIER_TYPE_NO = "05";

    private final PurchaseSupplierMapper supplierMapper;
    private final PurchaseSupplierTypeMapper supplierTypeMapper;
    private final PurchaseParameterMapper parameterMapper;
    private final StoreProperties storeProperties;

    public PurchaseSupplierService(
            PurchaseSupplierMapper supplierMapper,
            PurchaseSupplierTypeMapper supplierTypeMapper,
            PurchaseParameterMapper parameterMapper,
            StoreProperties storeProperties) {
        this.supplierMapper = supplierMapper;
        this.supplierTypeMapper = supplierTypeMapper;
        this.parameterMapper = parameterMapper;
        this.storeProperties = storeProperties;
    }

    public String resolveSupplierGuid(String fileName, String preferredSupplierName, LocalDateTime now) {
        String name = StrUtil.isNotBlank(preferredSupplierName)
                ? preferredSupplierName.trim()
                : extractSupplierName(fileName);
        if (StrUtil.isBlank(name)) {
            log.info("supplier name not found for [{}], use default supplier", fileName);
            return storeProperties.getProduct().getSupplierGuid();
        }
        Supplier match = findByFuzzyName(name);
        if (match != null) {
            log.info("supplier matched by name [{}] -> {} ({})", name, match.getNo(), match.getGuid());
            return match.getGuid();
        }
        String guid = createSupplier(name, now);
        log.info("supplier not found by name [{}], created new supplier {}", name, guid);
        return guid;
    }

    public static String extractSupplierName(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return "";
        }
        String base = fileName;
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        base = base.trim();
        if (base.isEmpty()) {
            return "";
        }
        base = base.replaceAll("(?i)[\\s\\u3000]*\\d{4}\\.\\d{1,2}.*$", "").trim();
        if (base.isEmpty()) {
            return "";
        }
        String[] parts = base.split("[\\s\\u3000]+", 2);
        return parts.length > 0 ? parts[0].trim() : "";
    }

    private Supplier findByFuzzyName(String name) {
        List<Supplier> list = supplierMapper.selectList(
                Wrappers.lambdaQuery(Supplier.class)
                        .and(w -> w.like(Supplier::getName, name).or().like(Supplier::getNameP, name))
                        .orderByAsc(Supplier::getNo));
        return list.isEmpty() ? null : list.get(0);
    }

    private String createSupplier(String name, LocalDateTime now) {
        boolean chinese = isChineseName(name);
        String typeGuid = resolveSupplierTypeGuid(chinese);
        StoreProperties.PurchaseDefaults defaults = storeProperties.getPurchase();
        String isoCountry = chinese
                ? defaults.getChineseSupplierIsoCountryCode()
                : defaults.getForeignSupplierIsoCountryCode();

        Supplier supplier = new Supplier();
        supplier.setGuid(UUID.randomUUID().toString());
        supplier.setTypeGuid(typeGuid);
        supplier.setNo(nextSupplierNo(typeGuid));
        if (chinese) {
            supplier.setName(name);
            supplier.setNameP("");
        } else {
            supplier.setName("");
            supplier.setNameP(name);
        }
        supplier.setDiscount(BigDecimal.ONE);
        supplier.setAccountLine(ZERO);
        supplier.setAccountTerm(0);
        supplier.setDebit(ZERO);
        supplier.setCredito(ZERO);
        supplier.setTransactionNumber(0);
        supplier.setTransactionAmount(ZERO);
        supplier.setSimPrnIva(false);
        supplier.setSimPrnCh(true);
        supplier.setIsoCountryCode(isoCountry);
        supplier.setBuildDate(now);
        supplier.setUpdateDate(now);
        supplier.setUpdateTime(now);
        supplier.setFussySearchKeyWord(name);
        supplierMapper.insert(supplier);
        return supplier.getGuid();
    }

    static boolean isChineseName(String name) {
        if (StrUtil.isBlank(name)) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            if (Character.UnicodeScript.of(name.charAt(i)) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    private String resolveSupplierTypeGuid(boolean chinese) {
        String typeNo = chinese ? CHINESE_SUPPLIER_TYPE_NO : FOREIGN_SUPPLIER_TYPE_NO;
        String typeGuid = StrUtil.trimToNull(supplierTypeMapper.selectGuidByNo(typeNo));
        if (typeGuid != null) {
            return typeGuid;
        }
        StoreProperties.PurchaseDefaults defaults = storeProperties.getPurchase();
        typeGuid = StrUtil.trimToNull(
                chinese ? defaults.getChineseSupplierTypeGuid() : defaults.getForeignSupplierTypeGuid());
        if (typeGuid != null) {
            log.warn("supplier type {} not found by No={}, fallback to configured GUID", typeNo, typeGuid);
            return typeGuid;
        }
        throw new IllegalStateException("supplier type not found for No=" + typeNo);
    }

    private String nextSupplierNo(String typeGuid) {
        String typeNo = StrUtil.trimToEmpty(supplierMapper.selectTypeNoByGuid(typeGuid));
        int seqLen = intParam(PARAM_AUTO_CODE_LENGTH, 6);
        boolean withTypePrefix = Boolean.TRUE.equals(parameterMapper.selectBoolValue(PARAM_WITH_TYPE_PREFIX));
        String prefix = withTypePrefix ? typeNo : "";
        int prefixLen = prefix.length();
        int codeLength = prefixLen + seqLen;

        String maxNo = supplierMapper.selectMaxNoByExactLength(prefix, prefixLen, codeLength);
        long nextSeq = 1L;
        if (StrUtil.isNotBlank(maxNo) && maxNo.length() == codeLength) {
            try {
                nextSeq = Long.parseLong(maxNo.substring(prefixLen)) + 1L;
            } catch (NumberFormatException ex) {
                throw new IllegalStateException("invalid supplier no suffix: " + maxNo, ex);
            }
        }

        String seq = String.format("%0" + seqLen + "d", nextSeq);
        if (seq.length() > seqLen) {
            throw new IllegalStateException("supplier no overflow for type " + typeNo + ", next=" + nextSeq);
        }
        return prefix + seq;
    }

    private int intParam(String name, int defaultValue) {
        Integer value = parameterMapper.selectIntValue(name);
        return value != null ? value : defaultValue;
    }
}
