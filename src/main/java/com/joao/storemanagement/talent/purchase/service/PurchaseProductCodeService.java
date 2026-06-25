package com.joao.storemanagement.talent.purchase.service;

import cn.hutool.core.util.StrUtil;
import com.joao.storemanagement.talent.purchase.mapper.PurchaseParameterMapper;
import com.joao.storemanagement.talent.purchase.mapper.PurchaseProductMapper;
import com.joao.storemanagement.talent.purchase.mapper.PurchaseProductTypeMapper;

public class PurchaseProductCodeService {

    private static final String PARAM_AUTO_CODE_LENGTH = "AutoCodeLength_Product";
    private static final String PARAM_WITH_TYPE_PREFIX = "AutoCodeWithTypeCodePrefix_Product";

    private final PurchaseProductMapper productMapper;
    private final PurchaseProductTypeMapper productTypeMapper;
    private final PurchaseParameterMapper parameterMapper;

    public PurchaseProductCodeService(
            PurchaseProductMapper productMapper,
            PurchaseProductTypeMapper productTypeMapper,
            PurchaseParameterMapper parameterMapper) {
        this.productMapper = productMapper;
        this.productTypeMapper = productTypeMapper;
        this.parameterMapper = parameterMapper;
    }

    public String nextProductNo(String typeGuid) {
        String typeNo = StrUtil.trimToEmpty(productTypeMapper.selectNoByGuid(typeGuid));
        if (typeNo.isEmpty()) {
            throw new IllegalStateException("product type not found: " + typeGuid);
        }

        int seqLen = intParam(PARAM_AUTO_CODE_LENGTH, 8);
        boolean withTypePrefix = Boolean.TRUE.equals(parameterMapper.selectBoolValue(PARAM_WITH_TYPE_PREFIX));
        String prefix = withTypePrefix ? typeNo : "";
        int prefixLen = prefix.length();
        int codeLength = prefixLen + seqLen;

        String maxNo = productMapper.selectMaxProductNoByExactLength(prefix, prefixLen, codeLength);

        long nextSeq = 1L;
        if (StrUtil.isNotBlank(maxNo) && maxNo.length() == codeLength) {
            String suffix = maxNo.substring(prefixLen);
            try {
                nextSeq = Long.parseLong(suffix) + 1L;
            } catch (NumberFormatException ex) {
                throw new IllegalStateException("invalid product no suffix: " + maxNo, ex);
            }
        }

        String seq = String.format("%0" + seqLen + "d", nextSeq);
        if (seq.length() > seqLen) {
            throw new IllegalStateException("product no overflow for type " + typeNo + ", next=" + nextSeq);
        }
        return prefix + seq;
    }

    private int intParam(String name, int defaultValue) {
        Integer value = parameterMapper.selectIntValue(name);
        return value != null ? value : defaultValue;
    }
}
