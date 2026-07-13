package com.joao.storemanagement.entity.primary;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.joao.storemanagement.enums.EmailInboxStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("email_inbox_attachments")
public class EmailInboxAttachment {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("message_uid")
    private String messageUid;

    @TableField("message_subject")
    private String messageSubject;

    @TableField("from_address")
    private String fromAddress;

    @TableField("received_at")
    private LocalDateTime receivedAt;

    @TableField("file_name")
    private String fileName;

    @TableField("extension_name")
    private String extensionName;

    @TableField("file_size")
    private Long fileSize;

    @TableField("stored_path")
    private String storedPath;

    @TableField("status")
    private EmailInboxStatus status;

    @TableField("import_attachment_uuid")
    private String importAttachmentUuid;

    @TableField("fetched_at")
    private LocalDateTime fetchedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
