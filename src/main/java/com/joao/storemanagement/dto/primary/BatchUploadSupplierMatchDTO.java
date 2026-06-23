package com.joao.storemanagement.dto.primary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BatchUploadSupplierMatchDTO {

    @NotEmpty(message = "fileNames 不能为空")
    private List<@NotBlank(message = "文件名不能为空") String> fileNames;
}
