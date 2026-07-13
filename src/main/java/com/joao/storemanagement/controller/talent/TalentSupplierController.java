package com.joao.storemanagement.controller.talent;

import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.security.RequirePermission;
import com.joao.storemanagement.security.SystemPermission;
import com.joao.storemanagement.entity.talent.Supplier;
import com.joao.storemanagement.service.talent.TalentOposDataSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/talentOpos/supplier")
@RequirePermission(SystemPermission.INVOICE_READ)
@RequiredArgsConstructor
@Tag(name = "TALENTOPOS供应商", description = "供应商下拉数据")
public class TalentSupplierController {

    private final TalentOposDataSourceService talentOposDataSourceService;

    @Operation(summary = "查询供应商列表")
    @GetMapping("/getList")
    public ApiResponse<List<Supplier>> list() {
        try {
            return ApiResponse.ok(talentOposDataSourceService.listSuppliers());
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("查询供应商列表", ex);
        }
    }
}
