package com.joao.storemanagement.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

@Getter
public enum AttachmentOwner implements IEnum<Integer> {

    SELF(0, "自己"),
    PARENT(1, "父母");

    private final Integer code;
    private final String description;

    AttachmentOwner(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public Integer getValue() {
        return code;
    }
}
