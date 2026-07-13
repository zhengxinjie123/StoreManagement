package com.joao.storemanagement.entity.primary;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("email_inbox_sync_records")
public class EmailInboxSyncRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private String sessionId;

    @TableField("user_id")
    private Long userId;

    @TableField("operator_name")
    private String operatorName;

    @TableField("sync_at")
    private LocalDateTime syncAt;

    @TableField("range_from_date")
    private LocalDate rangeFromDate;

    @TableField("range_to_date")
    private LocalDate rangeToDate;

    @TableField("scanned_messages")
    private Integer scannedMessages;

    @TableField("attachment_count")
    private Integer attachmentCount;

    @TableField("uploaded_count")
    private Integer uploadedCount;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
