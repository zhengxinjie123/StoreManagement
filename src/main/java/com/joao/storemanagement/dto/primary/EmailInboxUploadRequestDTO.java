package com.joao.storemanagement.dto.primary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record EmailInboxUploadRequestDTO(
        @NotNull(message = "同步记录不能为空") Long recordId,
        @NotEmpty(message = "请至少选择一个邮箱附件") List<EmailInboxUploadItemDTO> items,
        @NotNull(message = "ownerType 不能为空") String ownerType) {

    public record EmailInboxUploadItemDTO(
            @NotBlank(message = "附件 token 不能为空") String token,
            @NotBlank(message = "supplierGuid 不能为空") String supplierGuid) {}
}
