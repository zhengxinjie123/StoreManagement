package com.joao.storemanagement.dto.replenish;

import java.util.List;

public record ReplenishRecordTreeNodeDTO(String supplierName, List<ReplenishRecordItemDTO> items) {}
