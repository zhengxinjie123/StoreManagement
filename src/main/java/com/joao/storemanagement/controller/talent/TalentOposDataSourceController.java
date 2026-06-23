package com.joao.storemanagement.controller.talent;

import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.dto.talent.DataSourceDTO;
import com.joao.storemanagement.exceptions.BusinessException;
import com.joao.storemanagement.service.talent.TalentOposDataSourceService;
import com.joao.storemanagement.vo.talent.DataSourceStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/talentOpos/dataSource")
@RequiredArgsConstructor
@Tag(name = "TALENTOPOS数据源", description = "第三方库连接配置与验证")
public class TalentOposDataSourceController {

    private final TalentOposDataSourceService talentOposDataSourceService;

    @Operation(summary = "查询数据源状态")
    @GetMapping("/getStatus")
    public ApiResponse<DataSourceStatusVO> status() {
        return ApiResponse.ok(talentOposDataSourceService.status());
    }

    @Operation(summary = "保存数据源配置")
    @PostMapping("/save")
    public ApiResponse<DataSourceStatusVO> save(@Valid @RequestBody DataSourceDTO form) {
        try {
            return ApiResponse.ok("数据源配置成功", talentOposDataSourceService.save(form));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("保存数据源配置", ex);
        }
    }

    @Operation(summary = "验证数据源连接")
    @GetMapping("/verify")
    public ApiResponse<Map<String, Object>> verify() {
        try {
            return ApiResponse.ok("当前 TALENTOPOS 连接可用",
                    Map.of("productCount", talentOposDataSourceService.countProducts()));
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("验证数据源连接", ex);
        }
    }
}
