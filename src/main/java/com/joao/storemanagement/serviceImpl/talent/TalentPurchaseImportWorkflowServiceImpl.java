package com.joao.storemanagement.serviceImpl.talent;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.joao.storemanagement.config.StoreProperties;
import com.joao.storemanagement.dto.primary.BatchRetryImportDTO;
import com.joao.storemanagement.entity.primary.ImportAttachment;
import com.joao.storemanagement.entity.primary.InvoiceArchive;
import com.joao.storemanagement.enums.AttachmentOwner;
import com.joao.storemanagement.enums.ImportStatus;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.mapper.primary.ImportAttachmentMapper;
import com.joao.storemanagement.mapper.primary.InvoiceArchiveMapper;
import com.joao.storemanagement.service.primary.ImportAttachmentService;
import com.joao.storemanagement.service.primary.InvoiceCleanService;
import com.joao.storemanagement.service.talent.TalentPurchaseImportService;
import com.joao.storemanagement.service.talent.TalentPurchaseImportWorkflowService;
import com.joao.storemanagement.talent.purchase.dto.PurchaseImportResult;
import com.joao.storemanagement.utils.GuidHelper;
import com.joao.storemanagement.vo.primary.BatchRetryImportItemVO;
import com.joao.storemanagement.vo.primary.BatchRetryImportResultVO;
import com.joao.storemanagement.vo.primary.InvoiceArchiveVO;
import com.joao.storemanagement.vo.talent.PurchaseImportResultVO;
import com.joao.storemanagement.vo.talent.TalentPurchaseImportWorkflowVO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TalentPurchaseImportWorkflowServiceImpl implements TalentPurchaseImportWorkflowService {

    private final ImportAttachmentMapper importAttachmentMapper;
    private final InvoiceArchiveMapper invoiceArchiveMapper;
    private final InvoiceCleanService invoiceCleanService;
    private final TalentPurchaseImportService talentPurchaseImportService;
    private final ImportAttachmentService importAttachmentService;
    private final StoreProperties storeProperties;

    @Override
    public TalentPurchaseImportWorkflowVO importAttachment(String attachmentUuid, Long templateId) {
        return runImport(attachmentUuid, templateId, false);
    }

    @Override
    public TalentPurchaseImportWorkflowVO retryImport(String attachmentUuid, Long templateId) {
        ImportAttachment attachment = requireAttachment(attachmentUuid);
        if (!ImportStatus.FAILED.equals(attachment.getImportStatus())) {
            throw new BusinessException("仅导入失败的电子发票可重试");
        }
        return runImport(attachmentUuid, templateId, true);
    }

    @Override
    public BatchRetryImportResultVO batchRetryImport(BatchRetryImportDTO request) {
        List<BatchRetryImportItemVO> items = new ArrayList<>();
        int successCount = 0;
        for (String uuid : request.uuids()) {
            try {
                TalentPurchaseImportWorkflowVO result = retryImport(uuid, request.templateId());
                successCount++;
                items.add(BatchRetryImportItemVO.success(uuid, result));
            } catch (BusinessException ex) {
                items.add(BatchRetryImportItemVO.fail(uuid, ex.getMessage()));
            }
        }
        return BatchRetryImportResultVO.builder()
                .total(request.uuids().size())
                .successCount(successCount)
                .failureCount(request.uuids().size() - successCount)
                .items(items)
                .build();
    }

    @Override
    public TalentPurchaseImportWorkflowVO importArchive(String archiveUuid) {
        InvoiceArchive archive = requireArchive(archiveUuid);
        ImportAttachment attachment = requireAttachment(archive.getAttachmentUuid());
        if (ImportStatus.SUCCESS.equals(attachment.getImportStatus())) {
            throw new BusinessException("该发票已经导入成功");
        }
        ensureSelfAttachment(attachment);
        try {
            InvoiceArchiveVO archiveVo = InvoiceArchiveVO.of(archive, false, attachment);
            PurchaseImportResult purchase = talentPurchaseImportService.importFromArchive(
                    requireArchivePath(archiveVo),
                    archiveVo.getFileName() + "." + archiveVo.getExtensionName(),
                    attachment.getSupplierGuid(),
                    attachment.getFileName(),
                    archiveVo.getTaxIncluded());
            importAttachmentService.markImported(attachment.getUuid());
            return TalentPurchaseImportWorkflowVO.builder()
                    .cleanedBeforeImport(false)
                    .archive(archiveVo)
                    .purchase(toPurchaseVo(purchase))
                    .build();
        } catch (BusinessException ex) {
            importAttachmentService.markImportFailed(attachment.getUuid(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            importAttachmentService.markImportFailed(attachment.getUuid(), ex.getMessage());
            throw new BusinessException("导入失败: " + ex.getMessage(), ex);
        }
    }

    private TalentPurchaseImportWorkflowVO runImport(String attachmentUuid, Long templateId, boolean retry) {
        ImportAttachment attachment = requireAttachment(attachmentUuid);
        if (ImportStatus.SUCCESS.equals(attachment.getImportStatus())) {
            throw new BusinessException("该发票已经导入成功");
        }
        if (retry && !ImportStatus.FAILED.equals(attachment.getImportStatus())) {
            throw new BusinessException("仅导入失败的电子发票可重试");
        }
        ensureSelfAttachment(attachment);
        try {
            return doImport(attachment, templateId);
        } catch (BusinessException ex) {
            importAttachmentService.markImportFailed(attachment.getUuid(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            importAttachmentService.markImportFailed(attachment.getUuid(), ex.getMessage());
            throw new BusinessException("导入失败: " + ex.getMessage(), ex);
        }
    }

    private TalentPurchaseImportWorkflowVO doImport(ImportAttachment attachment, Long templateId) {
        boolean cleanedBeforeImport = false;
        InvoiceArchiveVO archive = findLatestArchiveVo(attachment.getUuid());
        if (archive == null) {
            if (templateId == null) {
                throw new BusinessException("请先清洗归档，或选择模板后再导入");
            }
            archive = invoiceCleanService.clean(attachment.getUuid(), attachment.getSupplierGuid(), templateId);
            cleanedBeforeImport = true;
        }
        PurchaseImportResult purchase = talentPurchaseImportService.importFromArchive(
                requireArchivePath(archive),
                archive.getFileName() + "." + archive.getExtensionName(),
                attachment.getSupplierGuid(),
                attachment.getFileName(),
                archive.getTaxIncluded());
        importAttachmentService.markImported(attachment.getUuid());
        return TalentPurchaseImportWorkflowVO.builder()
                .cleanedBeforeImport(cleanedBeforeImport)
                .archive(archive)
                .purchase(toPurchaseVo(purchase))
                .build();
    }

    private void ensureSelfAttachment(ImportAttachment attachment) {
        AttachmentOwner owner = attachment.getOwnerType() == null ? AttachmentOwner.SELF : attachment.getOwnerType();
        if (!AttachmentOwner.SELF.equals(owner)) {
            throw new BusinessException("只有自己的发票需要导入 TALENTOPOS，父母发票请使用 Google Drive 上传");
        }
    }

    private ImportAttachment requireAttachment(String attachmentUuid) {
        ImportAttachment attachment = importAttachmentMapper.selectById(GuidHelper.normalize(attachmentUuid));
        if (attachment == null) {
            throw new BusinessException("电子发票不存在: " + attachmentUuid);
        }
        return attachment;
    }

    private InvoiceArchive requireArchive(String archiveUuid) {
        InvoiceArchive archive = invoiceArchiveMapper.selectById(GuidHelper.normalize(archiveUuid));
        if (archive == null) {
            throw new BusinessException("归档记录不存在: " + archiveUuid);
        }
        return archive;
    }

    private InvoiceArchiveVO findLatestArchiveVo(String attachmentUuid) {
        InvoiceArchive archive = invoiceArchiveMapper.selectOne(Wrappers.lambdaQuery(InvoiceArchive.class)
                .eq(InvoiceArchive::getAttachmentUuid, GuidHelper.normalize(attachmentUuid))
                .orderByDesc(InvoiceArchive::getCreatedAt)
                .last("OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY"));
        return archive == null ? null : InvoiceArchiveVO.of(archive, false, (AttachmentOwner) null);
    }

    private Path requireArchivePath(InvoiceArchiveVO archive) {
        Path path = Path.of(storeProperties.getUpload().getInvoiceArchiveDir())
                .resolve(archive.getFilePath())
                .normalize();
        if (!Files.exists(path)) {
            throw new BusinessException("归档文件不存在: " + archive.getFileName());
        }
        return path;
    }

    private static PurchaseImportResultVO toPurchaseVo(PurchaseImportResult result) {
        return PurchaseImportResultVO.builder()
                .purchaseNo(result.purchaseNo())
                .purchaseGuid(result.purchaseGuid())
                .lineCount(result.lineCount())
                .newProductCount(result.newProductCount())
                .existingProductCount(result.existingProductCount())
                .build();
    }
}
