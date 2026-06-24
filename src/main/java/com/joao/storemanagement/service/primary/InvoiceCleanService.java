package com.joao.storemanagement.service.primary;

import com.joao.storemanagement.dto.primary.InvoiceCleanConfirmDTO;
import com.joao.storemanagement.vo.primary.InvoiceArchiveVO;
import com.joao.storemanagement.vo.primary.InvoiceCleanPreviewVO;

/**
 * 发票清洗服务。
 */
public interface InvoiceCleanService {

    /**
     * 清洗电子发票并直接生成归档（用于批量/一键清洗，无需人工预览）。
     */
    InvoiceArchiveVO clean(String attachmentUuid, String supplierGuid, Long templateId);

    /**
     * 试清洗，不写入归档，返回可供前端预览编辑的明细行与汇总。
     */
    InvoiceCleanPreviewVO preview(String attachmentUuid, Long templateId, String supplierGuid);

    /**
     * 预览确认后，按前端编辑的明细行写出并归档。
     */
    InvoiceArchiveVO confirmArchive(String attachmentUuid, InvoiceCleanConfirmDTO form);
}
