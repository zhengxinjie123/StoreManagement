package com.joao.storemanagement.utils;

import cn.hutool.core.util.StrUtil;
import com.joao.storemanagement.exception.BusinessException;

/**
 * 条码工具类
 */
public final class BarcodeUtil {


    /**
     * 校验条码是否符合规范
     *
     * @param barcode 条形码
     * @return 条码
     */
    public static String judgeBarcode(String barcode) {
        // 去除首尾空格，null 转为空字符串
        barcode = StrUtil.trimToEmpty(barcode);
        if (barcode.isEmpty()) {
            return barcode;
        }
        // 判断条码是否是科学计数法表示
        if (barcode.contains("e") || barcode.contains("E")) {
            throw new BusinessException("条码不能为科学计数法，请调整后重试。");
        }

        // 判断条码是否存在小数点
        int dot = barcode.indexOf('.');
        if (dot >= 0) {
            throw new BusinessException("条码不能有小数点，请调整后重试。");
        }
        return barcode;
    }
}
