package com.joao.storemanagement.dto.replenish;

import java.util.List;

public record PurchaseSuggestionGroupDTO(String supplierName, List<PurchaseSuggestionItemDTO> items) {}
