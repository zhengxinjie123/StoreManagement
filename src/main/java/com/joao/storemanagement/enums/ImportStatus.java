package com.joao.storemanagement.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

@Getter
public enum ImportStatus implements IEnum<Integer> {

    FAILED(0, "导入失败"),
    SUCCESS(1, "导入成功"),
    NOT_IMPORTED(-1, "未导入");

    private final Integer code;
    private final String description;

    ImportStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public Integer getValue() {
        return code;
    }
}
