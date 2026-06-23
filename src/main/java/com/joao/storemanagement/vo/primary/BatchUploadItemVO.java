package com.joao.storemanagement.vo.primary;

import com.joao.storemanagement.enums.UploadResultMessage;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BatchUploadItemVO {

    private final String fileName;
    private final boolean success;
    private final String message;
    private final AttachmentVO attachment;

    public static BatchUploadItemVO success(String fileName, AttachmentVO attachment) {
        return BatchUploadItemVO.builder()
                .fileName(fileName)
                .success(true)
                .message(UploadResultMessage.SUCCESS.getText())
                .attachment(attachment)
                .build();
    }

    public static BatchUploadItemVO fail(String fileName, String message) {
        return BatchUploadItemVO.builder()
                .fileName(fileName)
                .success(false)
                .message(message)
                .build();
    }
}
