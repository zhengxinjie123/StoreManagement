package com.joao.storemanagement.serviceImpl.pricing;

import cn.hutool.core.util.StrUtil;
import com.joao.storemanagement.dto.pricing.RetailPriceManageUpdateDTO;
import com.joao.storemanagement.dto.talent.ProductDTO;
import com.joao.storemanagement.entity.pricing.RetailPriceManualChange;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.mapper.primary.pricing.RetailPriceManualChangeMapper;
import com.joao.storemanagement.mapper.talent.PosProductPriceWriteMapper;
import com.joao.storemanagement.service.pricing.RetailPriceManageService;
import com.joao.storemanagement.service.talent.TalentOposDataSourceService;
import com.joao.storemanagement.vo.pricing.RetailPriceManageItemVO;
import com.joao.storemanagement.vo.pricing.RetailPriceManualChangeVO;
import com.joao.storemanagement.vo.response.PageResponseVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RetailPriceManageServiceImpl implements RetailPriceManageService {

    private final TalentOposDataSourceService talentOposDataSourceService;
    private final RetailPriceManualChangeMapper manualChangeMapper;

    public RetailPriceManageServiceImpl(
            TalentOposDataSourceService talentOposDataSourceService,
            RetailPriceManualChangeMapper manualChangeMapper) {
        this.talentOposDataSourceService = talentOposDataSourceService;
        this.manualChangeMapper = manualChangeMapper;
    }

    @Override
    public PageResponseVO<RetailPriceManageItemVO> pageMissingRetail(long current, long pageSize) {
        long safeCurrent = Math.max(current, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 200);
        long offset = (safeCurrent - 1) * safePageSize;
        long total = countMissingRetailPrice();
        List<RetailPriceManageItemVO> records = talentOposDataSourceService
                .executeInSession(session -> session.getMapper(PosProductPriceWriteMapper.class)
                        .selectMissingRetailPricePage(offset, safePageSize))
                .stream()
                .map(this::toItem)
                .toList();
        return PageResponseVO.of(safeCurrent, safePageSize, total, records);
    }

    @Override
    @Transactional
    public void updateRetailPrice(String productGuid, RetailPriceManageUpdateDTO request, String username) {
        ProductDTO product = selectProductByGuid(productGuid);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (hasRetailPrice(product.retailPrice())) {
            throw new BusinessException("该商品已有零售价，请刷新列表");
        }
        BigDecimal retailPriceTax = request.retailPriceTax();
        BigDecimal retailPrice = calcPriceExTax(retailPriceTax, product.taxRate());
        int updated = talentOposDataSourceService.executeInWriteTransaction(session ->
                session.getMapper(PosProductPriceWriteMapper.class)
                        .updateRetailPrice(productGuid, retailPriceTax, retailPrice));
        if (updated <= 0) {
            throw new BusinessException("零售价更新失败，请刷新后重试");
        }
        RetailPriceManualChange change = new RetailPriceManualChange();
        change.setProductGuid(product.guid());
        change.setBarcode(product.barcode());
        change.setProductName(displayName(product));
        change.setSupplierName(product.supplierName());
        change.setOldRetailPriceTax(product.retailPrice());
        change.setNewRetailPriceTax(retailPriceTax);
        change.setUsername(username);
        change.setRolledBack(false);
        change.setCreatedAt(LocalDateTime.now());
        manualChangeMapper.insert(change);
    }

    @Override
    public PageResponseVO<RetailPriceManualChangeVO> pageChanges(long current, long pageSize) {
        long safeCurrent = Math.max(current, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 200);
        long offset = (safeCurrent - 1) * safePageSize;
        long total = manualChangeMapper.countAll();
        List<RetailPriceManualChangeVO> records = manualChangeMapper.selectPage(offset, safePageSize).stream()
                .map(RetailPriceManualChangeVO::of)
                .toList();
        return PageResponseVO.of(safeCurrent, safePageSize, total, records);
    }

    @Override
    @Transactional
    public void rollbackChange(Long changeId, String username) {
        RetailPriceManualChange change = manualChangeMapper.selectById(changeId);
        if (change == null) {
            throw new BusinessException("操作记录不存在");
        }
        if (Boolean.TRUE.equals(change.getRolledBack())) {
            throw new BusinessException("该记录已回滚");
        }
        ProductDTO product = selectProductByGuid(change.getProductGuid());
        if (product == null) {
            throw new BusinessException("商品不存在，无法回滚");
        }
        BigDecimal restoreTax = change.getOldRetailPriceTax();
        BigDecimal restorePrice = restoreTax == null || restoreTax.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : calcPriceExTax(restoreTax, product.taxRate());
        int updated = talentOposDataSourceService.executeInWriteTransaction(session ->
                session.getMapper(PosProductPriceWriteMapper.class)
                        .updateRetailPriceDirect(change.getProductGuid(), restoreTax, restorePrice));
        if (updated <= 0) {
            throw new BusinessException("回滚失败，请稍后重试");
        }
        manualChangeMapper.markRolledBack(changeId);
    }

    private long countMissingRetailPrice() {
        return talentOposDataSourceService.executeInSession(
                session -> session.getMapper(PosProductPriceWriteMapper.class).countMissingRetailPrice());
    }

    private ProductDTO selectProductByGuid(String productGuid) {
        return talentOposDataSourceService.executeInSession(
                session -> session.getMapper(PosProductPriceWriteMapper.class).selectByGuid(productGuid));
    }

    private RetailPriceManageItemVO toItem(ProductDTO product) {
        return RetailPriceManageItemVO.builder()
                .productGuid(product.guid())
                .barcode(product.barcode())
                .productName(displayName(product))
                .supplierName(product.supplierName())
                .purchasePrice(product.purchasePrice())
                .purchasePriceTax(product.purchasePriceTax())
                .retailPriceTax(product.retailPrice())
                .build();
    }

    private String displayName(ProductDTO product) {
        if (StrUtil.isNotBlank(product.nameForeign())) {
            return product.nameForeign();
        }
        return StrUtil.blankToDefault(product.nameChinese(), "—");
    }

    private boolean hasRetailPrice(BigDecimal retailPriceTax) {
        return retailPriceTax != null && retailPriceTax.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal calcPriceExTax(BigDecimal priceTax, BigDecimal taxRate) {
        BigDecimal rate = taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0
                ? taxRate
                : new BigDecimal("0.23");
        return priceTax.divide(BigDecimal.ONE.add(rate), 4, RoundingMode.HALF_UP);
    }
}
