package com.joao.storemanagement.talent.purchase.exception;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/** 导入单行校验/处理失败，携带行号与条码，便于生成大白话提示。 */
@Getter
public class ImportRowException extends IllegalArgumentException {

    private final int rowNumber;
    private final String barcode;

    public ImportRowException(int rowNumber, String barcode, String reason) {
        super(reason);
        this.rowNumber = rowNumber;
        this.barcode = StrUtil.trimToEmpty(barcode);
    }

    public String toFriendlyMessage() {
        String code = StrUtil.isBlank(barcode) ? "（空）" : barcode;
        return "第" + rowNumber + "行，条码 " + code + "：" + getMessage();
    }
}
