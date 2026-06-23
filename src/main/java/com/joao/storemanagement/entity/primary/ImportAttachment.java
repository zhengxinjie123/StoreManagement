package com.joao.storemanagement.entity.primary;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.joao.storemanagement.enums.AttachmentOwner;
import com.joao.storemanagement.enums.CleanStatus;
import com.joao.storemanagement.enums.ImportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("import_attachments")
public class ImportAttachment {

    @TableId(value = "uuid", type = IdType.ASSIGN_UUID)
    private String uuid;

    @TableField("file_name")
    private String fileName;

    @TableField("file_size")
    private Long fileSize;

    @TableField("upload_date")
    private LocalDateTime uploadDate;

    @TableField("extension_name")
    private String extensionName;

    @TableField("supplier_guid")
    private String supplierGuid;

    @TableField("import_status")
    private ImportStatus importStatus;

    @TableField("owner_type")
    private AttachmentOwner ownerType;

    @TableField("clean_status")
    private CleanStatus cleanStatus;

}
