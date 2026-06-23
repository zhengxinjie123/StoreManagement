package com.joao.storemanagement.service.primary;

import com.joao.storemanagement.vo.primary.InvoiceArchiveVO;
import com.joao.storemanagement.vo.primary.InvoiceCleanPreviewVO;

/**
 * 发票清洗服务。
 */
public interface InvoiceCleanService {

    /**
     * 清洗电子发票并生成归档。
     */
    InvoiceArchiveVO clean(String attachmentUuid, String supplierGuid, Long templateId);

    /**
     * 试清洗，不写入归档。
     */
    InvoiceCleanPreviewVO preview(String attachmentUuid, Long templateId, String supplierGuid);
}
