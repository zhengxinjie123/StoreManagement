package com.joao.storemanagement.service.primary;

import com.joao.storemanagement.enums.AttachmentOwner;
import com.joao.storemanagement.enums.CleanStatus;
import com.joao.storemanagement.enums.ImportStatus;
import com.joao.storemanagement.vo.primary.AttachmentVO;
import com.joao.storemanagement.vo.primary.BatchUploadResultVO;
import com.joao.storemanagement.vo.primary.BatchUploadSupplierMatchVO;
import com.joao.storemanagement.vo.primary.DownloadFileVO;
import com.joao.storemanagement.vo.response.PageResponseVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 电子发票附件服务。
 */
public interface ImportAttachmentService {

    /**
     * 分页查询电子发票附件。
     */
    PageResponseVO<AttachmentVO> page(long current, long pageSize, String supplierGuid,
                                      AttachmentOwner ownerType, CleanStatus cleanStatus,
                                      ImportStatus importStatus);

    /**
     * 上传单个电子发票附件。
     */
    AttachmentVO upload(MultipartFile file, String supplierGuid, AttachmentOwner ownerType);

    /**
     * 批量上传电子发票附件。
     */
    BatchUploadResultVO batchUpload(List<MultipartFile> files, List<String> supplierGuids, AttachmentOwner ownerType);

    /**
     * 根据文件名批量匹配供应商（文件名包含供应商名称即视为匹配）。
     */
    List<BatchUploadSupplierMatchVO> matchSuppliersByFileName(List<String> fileNames);

    /**
     * 获取附件下载信息。
     */
    DownloadFileVO getDownloadFile(String uuid);

    /**
     * 删除电子发票附件。
     */
    void delete(String uuid);

    /**
     * 标记电子发票为已清洗。
     */
    void markCleaned(String uuid);
}
