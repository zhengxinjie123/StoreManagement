package com.joao.storemanagement.vo.primary;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BatchUploadResultVO {

    private final int total;
    private final int successCount;
    private final int failureCount;
    private final List<BatchUploadItemVO> items;
}
