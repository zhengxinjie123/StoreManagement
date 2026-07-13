package com.joao.storemanagement.vo.primary;

import com.joao.storemanagement.vo.talent.TalentPurchaseImportWorkflowVO;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BatchRetryImportItemVO {

    private final String uuid;
    private final boolean success;
    private final String message;
    private final TalentPurchaseImportWorkflowVO result;

    public static BatchRetryImportItemVO success(String uuid, TalentPurchaseImportWorkflowVO result) {
        return BatchRetryImportItemVO.builder().uuid(uuid).success(true).result(result).build();
    }

    public static BatchRetryImportItemVO fail(String uuid, String message) {
        return BatchRetryImportItemVO.builder().uuid(uuid).success(false).message(message).build();
    }
}
