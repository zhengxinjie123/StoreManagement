package com.joao.storemanagement.vo.pricing;

import com.joao.storemanagement.entity.pricing.RetailPriceSyncRun;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RetailPriceSyncRunVO {

    private final Long id;
    private final Integer matchedCount;
    private final Integer appliedCount;
    private final Integer skippedCount;
    private final String remark;
    private final Long rollbackOfRunId;
    private final LocalDateTime createdAt;

    public static RetailPriceSyncRunVO of(RetailPriceSyncRun run) {
        return RetailPriceSyncRunVO.builder()
                .id(run.getId())
                .matchedCount(run.getMatchedCount())
                .appliedCount(run.getAppliedCount())
                .skippedCount(run.getSkippedCount())
                .remark(run.getRemark())
                .rollbackOfRunId(run.getRollbackOfRunId())
                .createdAt(run.getCreatedAt())
                .build();
    }
}
