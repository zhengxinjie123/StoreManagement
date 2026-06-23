package com.joao.storemanagement.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

@Getter
public enum CleanStatus implements IEnum<Integer> {

    NOT_CLEANED(0, "未清洗"),
    CLEANED(1, "已清洗");

    private final Integer code;
    private final String description;

    CleanStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public Integer getValue() {
        return code;
    }
}
