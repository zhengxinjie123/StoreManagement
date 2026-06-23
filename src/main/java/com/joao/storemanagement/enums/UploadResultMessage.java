package com.joao.storemanagement.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 上传相关固定文案。
 */
@Getter
@RequiredArgsConstructor
public enum UploadResultMessage {

    SUCCESS("上传成功");

    private final String text;
}
