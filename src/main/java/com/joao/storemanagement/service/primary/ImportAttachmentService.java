package com.joao.storemanagement.service.primary;

import com.joao.storemanagement.enums.AttachmentOwner;
import com.joao.storemanagement.enums.CleanStatus;
import com.joao.storemanagement.enums.ImportStatus;
import com.joao.storemanagement.vo.primary.AttachmentVO;
import com.joao.storemanagement.vo.primary.BatchUploadResultVO;
import com.joao.storemanagement.vo.primary.BatchUploadSupplierMatchVO;
import com.joao.storemanagement.vo.primary.DownloadFileVO;
import com.joao.storemanagement.vo.response.PageResponseVO;
import java.time.LocalDate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 电子发票附件服务。
 */
public interface ImportAttachmentService {

    /**
     * 分页查询电子发票附件。
     */
    PageResponseVO<AttachmentVO> page(
            long current,
            long pageSize,
            String supplierGuid,
            AttachmentOwner ownerType,
            CleanStatus cleanStatus,
            ImportStatus importStatus,
            LocalDate fromUploadDate,
            LocalDate toUploadDate);

    AttachmentVO upload(MultipartFile file, String supplierGuid, AttachmentOwner ownerType);

    BatchUploadResultVO batchUpload(List<MultipartFile> files, List<String> supplierGuids, AttachmentOwner ownerType);

    List<BatchUploadSupplierMatchVO> matchSuppliersByFileName(List<String> fileNames);

    DownloadFileVO getDownloadFile(String uuid);

    void delete(String uuid);

    void markCleaned(String uuid);

    void markImported(String uuid);

    /**
     * 标记导入失败并记录失败原因。
     *
     * @param uuid   附件 UUID
     * @param reason 失败原因
     */
    void markImportFailed(String uuid, String reason);
}
