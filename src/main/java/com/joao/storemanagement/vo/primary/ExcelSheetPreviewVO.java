package com.joao.storemanagement.vo.primary;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ExcelSheetPreviewVO {

    private final String name;

    private final List<List<String>> rows;

    private final boolean truncated;
}
