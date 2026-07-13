package com.joao.storemanagement.service.primary;

import com.joao.storemanagement.dto.primary.EmailInboxUploadRequestDTO;
import com.joao.storemanagement.vo.primary.BatchUploadResultVO;
import com.joao.storemanagement.vo.primary.EmailInboxFetchResultVO;
import com.joao.storemanagement.vo.primary.EmailInboxPreviewFileVO;
import com.joao.storemanagement.vo.primary.EmailInboxSyncRecordVO;
import com.joao.storemanagement.vo.response.PageResponseVO;
import java.time.LocalDate;

public interface EmailInboxService {

    EmailInboxFetchResultVO fetch(LocalDate fromDate, LocalDate toDate);

    EmailInboxPreviewFileVO previewFile(String token);

    BatchUploadResultVO upload(EmailInboxUploadRequestDTO request);

    PageResponseVO<EmailInboxSyncRecordVO> listSyncRecords(long current, long pageSize);
}
