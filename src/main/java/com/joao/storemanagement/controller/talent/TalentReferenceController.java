package com.joao.storemanagement.controller.talent;

import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.exceptions.BusinessException;
import com.joao.storemanagement.service.talent.TalentOposDataSourceService;
import com.joao.storemanagement.vo.talent.TalentReferenceOptionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/talentOpos/reference")
@RequiredArgsConstructor
@Tag(name = "TALENTOPOS参考数据", description = "系统参数配置下拉选项")
public class TalentReferenceController {

    private final TalentOposDataSourceService talentOposDataSourceService;

    @Operation(summary = "按类型查询参考数据")
    @GetMapping("/getOptions")
    public ApiResponse<List<TalentReferenceOptionVO>> listByType(
            @RequestParam @NotBlank(message = "type 不能为空") String type) {
        try {
            return ApiResponse.ok(talentOposDataSourceService.listReferenceOptions(type));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("查询参考数据", ex);
        }
    }

    @Operation(summary = "查询全部参考数据")
    @GetMapping("/getOptionMap")
    public ApiResponse<Map<String, List<TalentReferenceOptionVO>>> listAll() {
        try {
            return ApiResponse.ok(talentOposDataSourceService.listReferenceOptionMap());
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("查询参考数据", ex);
        }
    }
}
