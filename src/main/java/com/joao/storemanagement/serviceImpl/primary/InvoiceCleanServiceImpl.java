package com.joao.storemanagement.serviceImpl.primary;

import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanRow;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanStrategy;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanStrategyFactory;
import com.joao.storemanagement.category.invoiceclean.InvoiceCleanSupport;
import com.joao.storemanagement.category.invoiceclean.InvoiceFooterSummary;
import com.joao.storemanagement.category.invoiceclean.InvoiceParseResult;
import com.joao.storemanagement.entity.primary.ImportAttachment;
import com.joao.storemanagement.entity.primary.InvoiceTemplate;
import com.joao.storemanagement.enums.InvoiceCleanExtension;
import com.joao.storemanagement.exceptions.BusinessException;
import com.joao.storemanagement.mapper.primary.ImportAttachmentMapper;
import com.joao.storemanagement.service.primary.ImportAttachmentService;
import com.joao.storemanagement.service.primary.InvoiceArchiveService;
import com.joao.storemanagement.service.primary.InvoiceCleanService;
import com.joao.storemanagement.service.primary.InvoiceTemplateService;
import com.joao.storemanagement.utils.GuidHelper;
import com.joao.storemanagement.vo.primary.InvoiceArchiveVO;
import com.joao.storemanagement.vo.primary.InvoiceCleanPreviewVO;
import com.joao.storemanagement.vo.primary.InvoiceCleanSummaryVO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceCleanServiceImpl implements InvoiceCleanService {

    private final ImportAttachmentMapper importAttachmentMapper;
    private final ImportAttachmentService importAttachmentService;
    private final InvoiceTemplateService invoiceTemplateService;
    private final InvoiceArchiveService invoiceArchiveService;
    private final InvoiceCleanStrategyFactory strategyFactory;

    @Value("${store.upload.attachment-dir:uploads/import-attachments}")
    private String attachmentDir;

    @Override
    public InvoiceArchiveVO clean(String attachmentUuid, String supplierGuid, Long templateId) {
        CleanResult result = executeClean(attachmentUuid, templateId, supplierGuid);
        Path outputFile;
        try {
            outputFile = Files.createTempFile("invoice-clean-", ".xlsx");
        } catch (IOException ex) {
            throw new BusinessException("创建清洗输出文件失败: " + ex.getMessage(), ex);
        }
        try {
            writeOutput(outputFile, result.rows());
            InvoiceArchiveVO archive = invoiceArchiveService.register(
                    result.attachment().getUuid(),
                    result.attachment().getSupplierGuid(),
                    outputFile,
                    InvoiceCleanExtension.XLSX.getExtension(),
                    result.rows().size(),
                    result.summary());
            // 清洗成功后更新附件清洗状态
            importAttachmentService.markCleaned(attachmentUuid);
            return archive;
        } finally {
            try {
                Files.deleteIfExists(outputFile);
            } catch (IOException ignored) {
                // 临时文件清理失败不影响主流程
            }
        }
    }

    @Override
    public InvoiceCleanPreviewVO preview(String attachmentUuid, Long templateId, String supplierGuid) {
        CleanResult result = executeClean(attachmentUuid, templateId, supplierGuid);
        return InvoiceCleanPreviewVO.builder()
                .summary(result.summary())
                .rowCount(result.rows().size())
                .templateName(result.template().getName())
                .taxIncluded(result.summary().getTaxIncluded())
                .build();
    }

    private CleanResult executeClean(String attachmentUuid, Long templateId, String supplierGuid) {
        if (StrUtil.isBlank(attachmentUuid)) {
            throw new BusinessException("attachmentUuid 不能为空");
        }
        if (templateId == null) {
            throw new BusinessException("templateId 不能为空");
        }
        ImportAttachment attachment = requireAttachment(attachmentUuid);
        if (!InvoiceCleanExtension.supports(attachment.getExtensionName())) {
            throw new BusinessException("试清洗仅支持 Excel 电子发票");
        }
        requireMatchingSupplier(attachment, supplierGuid);

        InvoiceTemplate template = invoiceTemplateService.requireTemplateForSupplier(templateId, supplierGuid);
        boolean taxIncluded = invoiceTemplateService.isTaxIncluded(template);
        Path sourceFile = Path.of(attachmentDir)
                .resolve(attachment.getUuid() + "." + attachment.getExtensionName());
        if (!Files.exists(sourceFile)) {
            throw new BusinessException("电子发票文件不存在: " + attachment.getFileName());
        }

        InvoiceCleanStrategy strategy = strategyFactory.resolve(supplierGuid);
        InvoiceParseResult parseResult;
        BigDecimal footerTotal;
        InvoiceFooterSummary invoiceFooter;
        try (var reader = ExcelUtil.getReader(sourceFile.toFile())) {
            Sheet sheet = resolveSheet(reader.getSheet(), template.getSheetName());
            parseResult = strategy.parse(sheet, template, taxIncluded);
            invoiceFooter = strategy.resolveFooterSummary(sheet, template, taxIncluded);
            footerTotal = invoiceFooter == null ? InvoiceCleanSupport.parseInvoiceTotal(sheet) : null;
        } catch (Exception ex) {
            throw new BusinessException("读取电子发票失败: " + ex.getMessage(), ex);
        }
        InvoiceCleanSummaryVO summary = InvoiceCleanSupport.buildSummary(
                parseResult.getRows(),
                taxIncluded,
                footerTotal,
                invoiceFooter,
                parseResult.getFilteredCount(),
                parseResult.getFilteredAmount(),
                parseResult.getBarcodeMappingRemark());
        return new CleanResult(attachment, template, parseResult.getRows(), summary);
    }

    private void writeOutput(Path outputFile, List<InvoiceCleanRow> rows) {
        try (ExcelWriter writer = ExcelUtil.getWriter(true)) {
            writer.writeHeadRow(List.of("条码", "外文名", "数量", "进价", "税率"));
            for (InvoiceCleanRow row : rows) {
                writer.writeRow(List.of(
                        row.getBarcode(),
                        row.getForeignName(),
                        row.getQuantity(),
                        row.getOutputPrice(),
                        row.getTaxRate()));
            }
            writer.flush(outputFile.toFile());
        } catch (Exception ex) {
            throw new BusinessException("写入清洗结果失败: " + ex.getMessage(), ex);
        }
    }

    private Sheet resolveSheet(Sheet defaultSheet, String sheetName) {
        if (StrUtil.isBlank(sheetName)) {
            return defaultSheet;
        }
        Sheet sheet = defaultSheet.getWorkbook().getSheet(sheetName);
        if (sheet == null) {
            throw new BusinessException("Sheet 不存在: " + sheetName);
        }
        return sheet;
    }

    private ImportAttachment requireAttachment(String attachmentUuid) {
        ImportAttachment attachment = importAttachmentMapper.selectById(attachmentUuid);
        if (attachment == null) {
            throw new BusinessException("电子发票不存在: " + attachmentUuid);
        }
        return attachment;
    }

    private void requireMatchingSupplier(ImportAttachment attachment, String supplierGuid) {
        if (StrUtil.isBlank(supplierGuid)) {
            throw new BusinessException("supplierGuid 不能为空");
        }
        String normalizedRequest = GuidHelper.normalize(supplierGuid);
        String normalizedAttachment = GuidHelper.normalize(attachment.getSupplierGuid());
        if (!normalizedRequest.equalsIgnoreCase(normalizedAttachment)) {
            throw new BusinessException("供应商与电子发票不一致");
        }
    }

    private record CleanResult(
            ImportAttachment attachment,
            InvoiceTemplate template,
            List<InvoiceCleanRow> rows,
            InvoiceCleanSummaryVO summary) {
    }
}
