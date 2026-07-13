package com.joao.storemanagement.dto.pricing;

import java.util.List;

public record RetailPriceExcelPreviewDTO(
        int headerRowIndex,
        List<String> columnLetters,
        List<String> headers,
        List<List<String>> sampleRows,
        int totalDataRows,
        RetailPriceColumnMappingDTO suggestedMapping) {}
