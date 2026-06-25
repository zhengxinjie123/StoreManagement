package com.joao.storemanagement.controller.talent;

import com.joao.storemanagement.dto.response.ApiResponse;
import com.joao.storemanagement.exceptions.BusinessException;
import com.joao.storemanagement.service.talent.TalentPurchaseImportService;
import com.joao.storemanagement.talent.purchase.dto.PurchaseImportResult;
import com.joao.storemanagement.exceptions.FailureMessages;
import com.joao.storemanagement.vo.talent.PurchaseImportResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/talentOpos/purchase")
@RequiredArgsConstructor
@Tag(name = "TALENTOPOS采购入库", description = "Excel 导入采购单并写入 TALENTOPOS")
public class TalentPurchaseImportController {

    private final TalentPurchaseImportService talentPurchaseImportService;

    @Operation(summary = "Excel 采购入库", description = "解析标准格式 Excel，创建采购单并更新库存")
    @PostMapping("/importExcel")
    public ApiResponse<PurchaseImportResultVO> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            PurchaseImportResult result = talentPurchaseImportService.importFromExcel(file);
            return ApiResponse.ok(toVo(result));
        } catch (com.joao.storemanagement.talent.purchase.exception.ImportRowException ex) {
            return ApiResponse.fail(400, ex.toFriendlyMessage());
        } catch (BusinessException ex) {
            return ApiResponse.operationFail("采购入库", ex);
        } catch (IllegalArgumentException ex) {
            return ApiResponse.fail(400, FailureMessages.format("采购入库", ex.getMessage()));
        }
    }

    private static PurchaseImportResultVO toVo(PurchaseImportResult result) {
        return PurchaseImportResultVO.builder()
                .purchaseNo(result.purchaseNo())
                .purchaseGuid(result.purchaseGuid())
                .lineCount(result.lineCount())
                .newProductCount(result.newProductCount())
                .existingProductCount(result.existingProductCount())
                .build();
    }
}
