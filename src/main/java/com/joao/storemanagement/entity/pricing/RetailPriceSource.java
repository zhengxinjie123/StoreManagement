package com.joao.storemanagement.entity.pricing;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RetailPriceSource {
    private Long id;
    private String code;
    private String name;
    private Integer priority;
    private String remark;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


