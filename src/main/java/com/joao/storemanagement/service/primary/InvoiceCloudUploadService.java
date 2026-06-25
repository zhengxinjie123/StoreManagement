package com.joao.storemanagement.service.primary;

import com.joao.storemanagement.vo.primary.InvoiceArchiveVO;

public interface InvoiceCloudUploadService {

    InvoiceArchiveVO uploadArchiveToGoogleDrive(String attachmentUuid);
}
