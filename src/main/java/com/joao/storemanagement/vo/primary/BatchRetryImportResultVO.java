package com.joao.storemanagement.vo.primary;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BatchRetryImportResultVO {

    private final int total;
    private final int successCount;
    private final int failureCount;
    private final List<BatchRetryImportItemVO> items;
}
