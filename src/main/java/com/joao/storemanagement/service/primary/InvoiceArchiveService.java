package com.joao.storemanagement.service.primary;

import com.joao.storemanagement.vo.primary.InvoiceArchiveSupplierVO;
import com.joao.storemanagement.vo.primary.InvoiceArchiveVO;
import com.joao.storemanagement.vo.primary.InvoiceCleanSummaryVO;
import com.joao.storemanagement.vo.primary.DownloadFileVO;
import com.joao.storemanagement.vo.response.PageResponseVO;

import java.nio.file.Path;

/**
 * 清洗归档服务。
 */
public interface InvoiceArchiveService {

    /**
     * 分页查询归档文件。
     */
    PageResponseVO<InvoiceArchiveVO> page(long current, long pageSize, String supplierGuid);

    /**
     * 分页查询有归档的供应商。
     */
    PageResponseVO<InvoiceArchiveSupplierVO> pageSuppliers(long current, long pageSize, String keyword);

    /**
     * 获取归档文件下载信息。
     */
    DownloadFileVO getDownloadFile(String uuid);

    /**
     * 删除归档文件。
     */
    void delete(String uuid);

    /**
     * 按附件删除关联归档。
     */
    void deleteByFileId(String attachmentUuid);

    /**
     * 注册清洗归档。
     */
    InvoiceArchiveVO register(String attachmentUuid, String supplierGuid, Path sourceFile,
                              String extensionName, int rowCount, InvoiceCleanSummaryVO summary);
}
