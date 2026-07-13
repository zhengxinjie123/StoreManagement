package com.joao.storemanagement.service.primary;

import com.joao.storemanagement.dto.primary.BatchArchiveDownloadDTO;
import com.joao.storemanagement.enums.AttachmentOwner;
import com.joao.storemanagement.enums.ImportStatus;
import com.joao.storemanagement.vo.primary.InvoiceArchiveSupplierVO;
import com.joao.storemanagement.vo.primary.InvoiceArchiveVO;
import com.joao.storemanagement.vo.primary.InvoiceCleanSummaryVO;
import com.joao.storemanagement.vo.primary.DownloadFileVO;
import com.joao.storemanagement.vo.response.PageResponseVO;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * 清洗归档服务。
 */
public interface InvoiceArchiveService {

    /**
     * 分页查询归档文件。
     */
    PageResponseVO<InvoiceArchiveVO> page(
            long current,
            long pageSize,
            String supplierGuid,
            AttachmentOwner ownerType,
            ImportStatus importStatus,
            LocalDate fromDate,
            LocalDate toDate);

    PageResponseVO<InvoiceArchiveSupplierVO> pageSuppliers(
            long current,
            long pageSize,
            String keyword,
            AttachmentOwner ownerType,
            ImportStatus importStatus);

    DownloadFileVO getDownloadFile(String uuid);

    void delete(String uuid);

    void deleteByFileId(String attachmentUuid);

    InvoiceArchiveVO register(String attachmentUuid, String supplierGuid, Path sourceFile,
                            String extensionName, int rowCount, InvoiceCleanSummaryVO summary);

    /**
     * 批量下载归档发票 ZIP。
     *
     * @param request 归档 UUID 列表
     * @return ZIP 字节内容
     */
    byte[] batchDownloadZip(BatchArchiveDownloadDTO request);
}
