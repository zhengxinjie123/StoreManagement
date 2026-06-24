package com.joao.storemanagement.serviceImpl.primary;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joao.storemanagement.category.invoiceclean.FooterSummaryMode;
import com.joao.storemanagement.config.StoreProperties;
import com.joao.storemanagement.dto.primary.InvoiceTemplateDTO;
import com.joao.storemanagement.entity.primary.InvoiceTemplate;
import com.joao.storemanagement.exceptions.BusinessException;
import com.joao.storemanagement.mapper.primary.InvoiceTemplateMapper;
import com.joao.storemanagement.service.primary.InvoiceTemplateService;
import com.joao.storemanagement.service.talent.TalentOposDataSourceService;
import com.joao.storemanagement.utils.GuidHelper;
import com.joao.storemanagement.vo.primary.InvoiceTemplateVO;
import com.joao.storemanagement.vo.response.PageResponseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class InvoiceTemplateServiceImpl implements InvoiceTemplateService {

    private static final Pattern EXCEL_COL = Pattern.compile("^[A-Z]{1,3}$");

    private final InvoiceTemplateMapper invoiceTemplateMapper;
    private final TalentOposDataSourceService talentOposDataSourceService;
    private final StoreProperties storeProperties;

    @Override
    public PageResponseVO<InvoiceTemplateVO> page(long current, long pageSize, String supplierGuid) {
        LambdaQueryWrapper<InvoiceTemplate> query = Wrappers.lambdaQuery(InvoiceTemplate.class)
                .eq(StrUtil.isNotBlank(supplierGuid), InvoiceTemplate::getSupplierGuid, supplierGuid)
                .orderByDesc(InvoiceTemplate::getUpdatedAt);
        Page<InvoiceTemplate> page = invoiceTemplateMapper.selectPage(Page.of(current, pageSize), query);
        List<InvoiceTemplateVO> records = page.getRecords().stream().map(InvoiceTemplateVO::of).toList();
        return PageResponseVO.of(page, records);
    }

    @Override
    public InvoiceTemplateVO get(Long id) {
        requireTemplateId(id);
        return InvoiceTemplateVO.of(requireTemplate(id));
    }

    @Override
    public List<InvoiceTemplateVO> listForSupplier(String supplierGuid) {
        if (StrUtil.isBlank(supplierGuid)) {
            throw new BusinessException("supplierGuid 不能为空");
        }
        List<InvoiceTemplate> templates = invoiceTemplateMapper.selectList(
                Wrappers.lambdaQuery(InvoiceTemplate.class)
                        .eq(InvoiceTemplate::getSupplierGuid, GuidHelper.normalize(supplierGuid))
                        .orderByDesc(InvoiceTemplate::getUpdatedAt));
        return templates.stream().map(InvoiceTemplateVO::of).toList();
    }

    @Override
    public InvoiceTemplate requireTemplateForSupplier(Long id, String supplierGuid) {
        requireTemplateId(id);
        if (StrUtil.isBlank(supplierGuid)) {
            throw new BusinessException("supplierGuid 不能为空");
        }
        InvoiceTemplate template = requireTemplate(id);
        if (!GuidHelper.normalize(template.getSupplierGuid())
                .equalsIgnoreCase(GuidHelper.normalize(supplierGuid))) {
            throw new BusinessException("模板与电子发票供应商不一致");
        }
        return template;
    }

    @Override
    public boolean isTaxIncluded(InvoiceTemplate template) {
        return Boolean.TRUE.equals(template.getTaxIncluded());
    }

    @Override
    public InvoiceTemplateVO create(InvoiceTemplateDTO form) {
        requireForm(form);
        talentOposDataSourceService.validateSupplier(form.getSupplierGuid());
        validateForm(form);
        LocalDateTime now = LocalDateTime.now();
        InvoiceTemplate entity = toEntity(form);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        invoiceTemplateMapper.insert(entity);
        return InvoiceTemplateVO.of(entity);
    }

    @Override
    public InvoiceTemplateVO update(Long id, InvoiceTemplateDTO form) {
        requireTemplateId(id);
        requireForm(form);
        InvoiceTemplate entity = requireTemplate(id);
        talentOposDataSourceService.validateSupplier(form.getSupplierGuid());
        validateForm(form);
        applyForm(entity, form);
        entity.setUpdatedAt(LocalDateTime.now());
        invoiceTemplateMapper.updateById(entity);
        return InvoiceTemplateVO.of(entity);
    }

    @Override
    public void delete(Long id) {
        requireTemplateId(id);
        if (invoiceTemplateMapper.deleteById(id) == 0) {
            throw new BusinessException("模板不存在: " + id);
        }
    }

    private void requireTemplateId(Long id) {
        if (id == null) {
            throw new BusinessException("模板 ID 不能为空");
        }
    }

    private void requireForm(InvoiceTemplateDTO form) {
        if (form == null) {
            throw new BusinessException("模板参数不能为空");
        }
    }

    private InvoiceTemplate requireTemplate(Long id) {
        InvoiceTemplate entity = invoiceTemplateMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("模板不存在: " + id);
        }
        return entity;
    }

    private void validateForm(InvoiceTemplateDTO form) {
        if (form.getDataStartRow() < form.getHeaderRow()) {
            throw new BusinessException("数据起始行不能小于表头起始行");
        }
        if (form.getTaxIncluded() == null) {
            throw new BusinessException("是否含税入库不能为空");
        }
        validateColumn("条码列", form.getBarcodeCol());
        validateColumn("外文名列", form.getForeignNameCol());
        validateColumn("数量列", form.getQuantityCol());
        if (StrUtil.isBlank(form.getPriceCol()) && StrUtil.isBlank(form.getPriceTaxIncludedCol())) {
            throw new BusinessException("进价列与含税价列至少填写一项");
        }
        if (StrUtil.isNotBlank(form.getPriceCol())) {
            validateColumn("进价列", form.getPriceCol());
        }
        if (StrUtil.isNotBlank(form.getChineseNameCol())) {
            validateColumn("中文名列", form.getChineseNameCol());
        }
        if (StrUtil.isNotBlank(form.getNewBarcodeCol())) {
            validateColumn("新条码列", form.getNewBarcodeCol());
        }
        if (StrUtil.isNotBlank(form.getTaxRateCol())) {
            validateColumn("税率列", form.getTaxRateCol());
        }
        if (StrUtil.isNotBlank(form.getPriceTaxIncludedCol())) {
            validateColumn("含税单价列", form.getPriceTaxIncludedCol());
        }
        if (StrUtil.isNotBlank(form.getLineSubtotalCol())) {
            validateColumn("行小计列", form.getLineSubtotalCol());
        }
        parseFooterSummaryMode(form.getFooterSummaryMode());
    }

    private void validateColumn(String label, String column) {
        String normalized = normalizeColumn(column);
        if (!EXCEL_COL.matcher(normalized).matches()) {
            throw new BusinessException(label + "格式无效: " + column);
        }
    }

    private InvoiceTemplate toEntity(InvoiceTemplateDTO form) {
        InvoiceTemplate entity = new InvoiceTemplate();
        applyForm(entity, form);
        return entity;
    }

    private void applyForm(InvoiceTemplate entity, InvoiceTemplateDTO form) {
        entity.setSupplierGuid(form.getSupplierGuid());
        entity.setName(form.getName());
        entity.setHeaderRow(form.getHeaderRow());
        entity.setDataStartRow(form.getDataStartRow());
        entity.setSheetName(StrUtil.blankToDefault(form.getSheetName(), null));
        entity.setBarcodeCol(normalizeColumn(form.getBarcodeCol()));
        entity.setNewBarcodeCol(StrUtil.isBlank(form.getNewBarcodeCol())
                ? null
                : normalizeColumn(form.getNewBarcodeCol()));
        entity.setForeignNameCol(normalizeColumn(form.getForeignNameCol()));
        entity.setChineseNameCol(StrUtil.isBlank(form.getChineseNameCol())
                ? null
                : normalizeColumn(form.getChineseNameCol()));
        entity.setQuantityCol(normalizeColumn(form.getQuantityCol()));
        entity.setPriceCol(StrUtil.isBlank(form.getPriceCol()) ? null : normalizeColumn(form.getPriceCol()));
        entity.setPriceTaxIncludedCol(StrUtil.isBlank(form.getPriceTaxIncludedCol())
                ? null
                : normalizeColumn(form.getPriceTaxIncludedCol()));
        entity.setTaxRateCol(StrUtil.isBlank(form.getTaxRateCol()) ? null : normalizeColumn(form.getTaxRateCol()));
        entity.setLineSubtotalCol(StrUtil.isBlank(form.getLineSubtotalCol())
                ? null
                : normalizeColumn(form.getLineSubtotalCol()));
        entity.setDefaultTaxRate(form.getDefaultTaxRate() == null
                ? storeProperties.getInvoice().getDefaultTaxRate()
                : form.getDefaultTaxRate());
        entity.setTaxIncluded(form.getTaxIncluded());
        entity.setFilterRowsWithoutBarcode(defaultTrue(form.getFilterRowsWithoutBarcode()));
        entity.setSplitMixedChineseForeignName(Boolean.TRUE.equals(form.getSplitMixedChineseForeignName()));
        entity.setStripCurrencyFromPrice(Boolean.TRUE.equals(form.getStripCurrencyFromPrice()));
        entity.setSkipZeroPricePalletRows(Boolean.TRUE.equals(form.getSkipZeroPricePalletRows()));
        entity.setBreakOnTaxableBase(Boolean.TRUE.equals(form.getBreakOnTaxableBase()));
        entity.setProductHasNewBarcode(Boolean.TRUE.equals(form.getProductHasNewBarcode()));
        entity.setSkipBarcodeNotEAN13(Boolean.TRUE.equals(form.getSkipBarcodeNotEAN13()));
        entity.setFooterSummaryMode(parseFooterSummaryMode(form.getFooterSummaryMode()));
        entity.setRemark(StrUtil.blankToDefault(form.getRemark(), null));
    }

    private String normalizeColumn(String column) {
        return StrUtil.blankToDefault(column, "").trim().toUpperCase();
    }

    private boolean defaultTrue(Boolean value) {
        return value == null || Boolean.TRUE.equals(value);
    }

    private FooterSummaryMode parseFooterSummaryMode(String mode) {
        if (StrUtil.isBlank(mode)) {
            return FooterSummaryMode.NONE;
        }
        try {
            return FooterSummaryMode.valueOf(mode.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("页脚解析模式无效: " + mode);
        }
    }
}
