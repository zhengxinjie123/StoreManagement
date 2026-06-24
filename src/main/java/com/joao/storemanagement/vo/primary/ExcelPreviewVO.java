package com.joao.storemanagement.vo.primary;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ExcelPreviewVO {

    private final List<ExcelSheetPreviewVO> sheets;
}
