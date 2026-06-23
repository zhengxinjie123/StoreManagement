package com.joao.storemanagement.category.invoiceclean;

import com.joao.storemanagement.utils.ExcelCellReader;
import org.apache.poi.ss.usermodel.Sheet;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.joao.storemanagement.category.invoiceclean.InvoiceCleanSupport.money;

/**
 * 解析页脚同时出现 {@code Sub Total:} 与 {@code Dto. Total:} 的发票整单折扣。
 * <p>
 * 适用于晨光、飞跃等同款 ERP 模板；与诚信等仅含 {@code Total:} 行、折扣由明细行推导的发票不同。
 */
public final class SubTotalDtoTotalFooterSupport {

    private static final Pattern SUB_TOTAL_PATTERN = Pattern.compile(
            "Sub\\s+Total:\\s*([\\d,.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DTO_TOTAL_PATTERN = Pattern.compile(
            "Dto\\.\\s*Total:\\s*([\\d,.]+)", Pattern.CASE_INSENSITIVE);

    private SubTotalDtoTotalFooterSupport() {
    }

    public static InvoiceFooterSummary parse(Sheet sheet) {
        BigDecimal subTotal = null;
        BigDecimal dtoTotal = null;
        int lastRow = sheet.getLastRowNum();
        int from = Math.max(0, lastRow - 30);
        for (int rowIndex = from; rowIndex <= lastRow; rowIndex++) {
            for (int col = 0; col <= 20; col++) {
                String text = ExcelCellReader.readString(sheet, rowIndex, columnLetter(col));
                Matcher subMatcher = SUB_TOTAL_PATTERN.matcher(text);
                if (subMatcher.find()) {
                    subTotal = parseMoneyToken(subMatcher.group(1));
                }
                Matcher dtoMatcher = DTO_TOTAL_PATTERN.matcher(text);
                if (dtoMatcher.find()) {
                    dtoTotal = parseMoneyToken(dtoMatcher.group(1));
                }
            }
        }
        if (subTotal == null || dtoTotal == null) {
            return null;
        }
        return InvoiceFooterSummary.builder()
                .amountBeforeDiscount(subTotal)
                .discountAmount(dtoTotal)
                .totalAmount(money(subTotal.subtract(dtoTotal)))
                .build();
    }

    private static BigDecimal parseMoneyToken(String token) {
        return money(new BigDecimal(token.replace(",", "")));
    }

    private static String columnLetter(int index) {
        int value = index + 1;
        StringBuilder builder = new StringBuilder();
        while (value > 0) {
            int remainder = (value - 1) % 26;
            builder.insert(0, (char) ('A' + remainder));
            value = (value - 1) / 26;
        }
        return builder.toString();
    }
}
