package com.joao.storemanagement.dto.primary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record EmailInboxImportRequestDTO(
        @NotEmpty(message = "请至少选择一个邮箱附件") List<EmailInboxImportItemDTO> items,
        @NotNull(message = "ownerType 不能为空") String ownerType) {

    public record EmailInboxImportItemDTO(
            @NotNull(message = "附件 ID 不能为空") Long id,
            @NotBlank(message = "supplierGuid 不能为空") String supplierGuid) {}
}
