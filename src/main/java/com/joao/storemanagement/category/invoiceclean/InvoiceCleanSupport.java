package com.joao.storemanagement.category.invoiceclean;

import cn.hutool.core.util.StrUtil;
import com.joao.storemanagement.vo.primary.InvoiceCleanSummaryVO;
import com.joao.storemanagement.entity.primary.InvoiceTemplate;
import com.joao.storemanagement.utils.ExcelCellReader;
import org.apache.poi.ss.usermodel.Sheet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InvoiceCleanSupport {

    public static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("23");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final Pattern TOTAL_PATTERN = Pattern.compile("Total:\\s*([\\d,.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CJK_PATTERN = Pattern.compile("[\\u4e00-\\u9fff\\u3400-\\u4dbf\\uff00-\\uffef]+");

    private InvoiceCleanSupport() {
    }

    public record NameParts(String chinese, String foreignName) {
    }

    public static boolean isValidBarcode(String barcode) {
        return StrUtil.isNotBlank(barcode) && !StrUtil.equalsIgnoreCase(StrUtil.trim(barcode), "Subtotal");
    }

    public static boolean hasTaxRateColumn(InvoiceTemplate template) {
        return StrUtil.isNotBlank(template.getTaxRateCol());
    }

    public static String joinName(String current, String extra) {
        return StrUtil.isBlank(current) ? extra.trim() : current + " " + extra.trim();
    }

    public static boolean isZeroPricePalletRow(NameParts nameParts, BigDecimal unitPriceExTax,
                                             BigDecimal unitPriceIncTax, boolean taxIncluded) {
        String name = StrUtil.trim(StrUtil.nullToEmpty(nameParts.chinese()) + " "
                + StrUtil.nullToEmpty(nameParts.foreignName()));
        if (!StrUtil.containsIgnoreCase(name, "PALLET")) {
            return false;
        }
        BigDecimal price = taxIncluded
                ? (unitPriceIncTax != null ? unitPriceIncTax : unitPriceExTax)
                : (unitPriceExTax != null ? unitPriceExTax : unitPriceIncTax);
        return price != null && price.compareTo(BigDecimal.ZERO) == 0;
    }

    public static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal readTaxRateFromColumn(Sheet sheet, int rowIndex, InvoiceTemplate template,
                                                   InvoiceCleanOptions options) {
        if (StrUtil.isBlank(template.getTaxRateCol())) {
            return null;
        }
        return readDecimal(sheet, rowIndex, template.getTaxRateCol(), false);
    }

    public static BigDecimal readLineSubtotal(Sheet sheet, int rowIndex, InvoiceTemplate template,
                                              InvoiceCleanOptions options) {
        if (StrUtil.isBlank(template.getLineSubtotalCol())) {
            return null;
        }
        return readDecimal(sheet, rowIndex, template.getLineSubtotalCol(), options.isStripCurrencyFromPrice());
    }

    public static BigDecimal readUnitPriceExTax(Sheet sheet, int rowIndex, InvoiceTemplate template,
                                                InvoiceCleanOptions options) {
        if (StrUtil.isBlank(template.getPriceCol())) {
            return null;
        }
        return readDecimal(sheet, rowIndex, template.getPriceCol(), options.isStripCurrencyFromPrice());
    }

    public static BigDecimal readUnitPriceIncTax(Sheet sheet, int rowIndex, InvoiceTemplate template,
                                                 InvoiceCleanOptions options) {
        if (StrUtil.isBlank(template.getPriceTaxIncludedCol())) {
            return null;
        }
        return readDecimal(sheet, rowIndex, template.getPriceTaxIncludedCol(), options.isStripCurrencyFromPrice());
    }

    private static BigDecimal readDecimal(Sheet sheet, int rowIndex, String column, boolean stripCurrency) {
        return ExcelCellReader.readDecimal(sheet, rowIndex, column, stripCurrency);
    }

    public static BigDecimal calcTaxIncludedPrice(BigDecimal unitPriceExTax, BigDecimal taxRate) {
        BigDecimal multiplier = BigDecimal.ONE.add(taxRate.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP));
        return unitPriceExTax.multiply(multiplier).setScale(4, RoundingMode.HALF_UP);
    }

    public static BigDecimal calcTaxExcludedPrice(BigDecimal unitPriceIncTax, BigDecimal taxRate) {
        BigDecimal divisor = BigDecimal.ONE.add(taxRate.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP));
        return unitPriceIncTax.divide(divisor, 4, RoundingMode.HALF_UP);
    }

    public static BigDecimal calcLineTotalIncTax(BigDecimal lineSubtotalExTax, BigDecimal taxRate) {
        BigDecimal multiplier = BigDecimal.ONE.add(taxRate.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP));
        return lineSubtotalExTax.multiply(multiplier);
    }

    public static NameParts resolveName(Sheet sheet, int rowIndex, InvoiceTemplate template) {
        String rawName = ExcelCellReader.readString(sheet, rowIndex, template.getForeignNameCol());
        if (StrUtil.isNotBlank(template.getChineseNameCol())) {
            String chinese = ExcelCellReader.readString(sheet, rowIndex, template.getChineseNameCol());
            return new NameParts(StrUtil.trim(chinese), StrUtil.trim(rawName));
        }
        return new NameParts("", StrUtil.trim(rawName));
    }

    public static NameParts splitMixedName(String raw) {
        if (StrUtil.isBlank(raw)) {
            return new NameParts("", "");
        }
        String trimmed = raw.trim();
        StringBuilder chinese = new StringBuilder();
        StringBuilder foreign = new StringBuilder();
        Matcher matcher = CJK_PATTERN.matcher(trimmed);
        int last = 0;
        while (matcher.find()) {
            if (matcher.start() > last) {
                appendToken(foreign, trimmed.substring(last, matcher.start()));
            }
            appendToken(chinese, matcher.group());
            last = matcher.end();
        }
        if (last < trimmed.length()) {
            appendToken(foreign, trimmed.substring(last));
        }
        if (chinese.isEmpty() && foreign.isEmpty()) {
            return new NameParts("", trimmed);
        }
        return new NameParts(chinese.toString().trim(), foreign.toString().trim());
    }

    public static void appendName(List<InvoiceCleanRow> rows, NameParts nameParts) {
        if (rows.isEmpty()) {
            return;
        }
        if (StrUtil.isBlank(nameParts.chinese()) && StrUtil.isBlank(nameParts.foreignName())) {
            return;
        }
        InvoiceCleanRow last = rows.get(rows.size() - 1);
        last.setChineseName(joinName(last.getChineseName(), nameParts.chinese()));
        last.setForeignName(joinName(last.getForeignName(), nameParts.foreignName()));
    }

    public static BigDecimal parseInvoiceTotal(Sheet sheet) {
        int lastRow = sheet.getLastRowNum();
        int from = Math.max(0, lastRow - 30);
        for (int rowIndex = from; rowIndex <= lastRow; rowIndex++) {
            for (int col = 0; col <= 20; col++) {
                String text = ExcelCellReader.readString(sheet, rowIndex, columnLetter(col));
                Matcher matcher = TOTAL_PATTERN.matcher(text);
                if (matcher.find()) {
                    return parseMoneyToken(matcher.group(1));
                }
            }
        }
        return null;
    }

    public static InvoiceCleanSummaryVO buildSummary(List<InvoiceCleanRow> rows, boolean taxIncluded,
                                                 BigDecimal footerTotal, int filteredCount,
                                                 BigDecimal filteredAmount) {
        return buildSummary(rows, taxIncluded, footerTotal, null, filteredCount, filteredAmount, null);
    }

    public static InvoiceCleanSummaryVO buildSummary(List<InvoiceCleanRow> rows, boolean taxIncluded,
                                                 BigDecimal footerTotal,
                                                 InvoiceFooterSummary invoiceFooter,
                                                 int filteredCount,
                                                 BigDecimal filteredAmount) {
        return buildSummary(rows, taxIncluded, footerTotal, invoiceFooter, filteredCount, filteredAmount, null);
    }

    public static InvoiceCleanSummaryVO buildSummary(List<InvoiceCleanRow> rows, boolean taxIncluded,
                                                 BigDecimal footerTotal,
                                                 InvoiceFooterSummary invoiceFooter,
                                                 int filteredCount,
                                                 BigDecimal filteredAmount,
                                                 String barcodeMappingRemark) {
        if (invoiceFooter != null && invoiceFooter.hasInvoiceLevelDiscount()) {
            return buildInvoiceFooterSummary(rows, taxIncluded, invoiceFooter, filteredCount, filteredAmount,
                    barcodeMappingRemark);
        }
        if (taxIncluded) {
            return buildTaxIncludedSummary(rows, footerTotal, filteredCount, filteredAmount, barcodeMappingRemark);
        }
        return buildTaxExcludedSummary(rows, footerTotal, filteredCount, filteredAmount, barcodeMappingRemark);
    }

    private static InvoiceCleanSummaryVO buildInvoiceFooterSummary(List<InvoiceCleanRow> rows,
                                                                 boolean taxIncluded,
                                                                 InvoiceFooterSummary invoiceFooter,
                                                                 int filteredCount,
                                                                 BigDecimal filteredAmount,
                                                                 String barcodeMappingRemark) {
        BigDecimal totalQuantity = BigDecimal.ZERO;
        for (InvoiceCleanRow row : rows) {
            totalQuantity = totalQuantity.add(row.getQuantity());
        }
        return InvoiceCleanSummaryVO.builder()
                .totalQuantity(totalQuantity.setScale(4, RoundingMode.HALF_UP))
                .amountBeforeDiscount(invoiceFooter.getAmountBeforeDiscount())
                .discountAmount(invoiceFooter.getDiscountAmount())
                .totalAmount(invoiceFooter.getTotalAmount())
                .taxIncluded(taxIncluded)
                .filteredCount(filteredCount)
                .filteredAmount(filteredAmount)
                .remark(buildCleanRemark(filteredCount, filteredAmount, barcodeMappingRemark))
                .build();
    }

    private static InvoiceCleanSummaryVO buildTaxIncludedSummary(List<InvoiceCleanRow> rows,
                                                                 BigDecimal footerTotal,
                                                                 int filteredCount,
                                                                 BigDecimal filteredAmount,
                                                                 String barcodeMappingRemark) {
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal amountBeforeDiscount = BigDecimal.ZERO;
        BigDecimal subtotalIncTax = BigDecimal.ZERO;

        for (InvoiceCleanRow row : rows) {
            totalQuantity = totalQuantity.add(row.getQuantity());
            amountBeforeDiscount = amountBeforeDiscount.add(row.getUnitPriceIncTax().multiply(row.getQuantity()));
            subtotalIncTax = subtotalIncTax.add(row.getLineSubtotalIncTax());
        }

        BigDecimal discountAmount = amountBeforeDiscount.subtract(subtotalIncTax);
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            discountAmount = BigDecimal.ZERO;
        }

        BigDecimal totalAmount = money(subtotalIncTax);
        if (footerTotal != null) {
            totalAmount = footerTotal;
        }

        return InvoiceCleanSummaryVO.builder()
                .totalQuantity(totalQuantity.setScale(4, RoundingMode.HALF_UP))
                .amountBeforeDiscount(money(amountBeforeDiscount))
                .discountAmount(money(discountAmount))
                .totalAmount(totalAmount)
                .taxIncluded(true)
                .filteredCount(filteredCount)
                .filteredAmount(filteredAmount)
                .remark(buildCleanRemark(filteredCount, filteredAmount, barcodeMappingRemark))
                .build();
    }

    private static InvoiceCleanSummaryVO buildTaxExcludedSummary(List<InvoiceCleanRow> rows,
                                                               BigDecimal footerTotal,
                                                               int filteredCount,
                                                               BigDecimal filteredAmount,
                                                               String barcodeMappingRemark) {
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal amountBeforeDiscount = BigDecimal.ZERO;
        BigDecimal subtotalExTax = BigDecimal.ZERO;
        Map<BigDecimal, BigDecimal> groupedSubtotal = new LinkedHashMap<>();

        for (InvoiceCleanRow row : rows) {
            BigDecimal lineGrossExTax = row.getUnitPriceExTax().multiply(row.getQuantity());
            totalQuantity = totalQuantity.add(row.getQuantity());
            amountBeforeDiscount = amountBeforeDiscount.add(lineGrossExTax);
            subtotalExTax = subtotalExTax.add(row.getLineSubtotalExTax());
            groupedSubtotal.merge(row.getTaxRate(), row.getLineSubtotalExTax(), BigDecimal::add);
        }

        BigDecimal discountAmount = amountBeforeDiscount.subtract(subtotalExTax);
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            discountAmount = BigDecimal.ZERO;
        }

        BigDecimal totalAmount = groupedSubtotal.entrySet()
                .stream()
                .map(entry -> money(calcLineTotalIncTax(entry.getValue(), entry.getKey())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (footerTotal != null) {
            totalAmount = footerTotal;
        }

        return InvoiceCleanSummaryVO.builder()
                .totalQuantity(totalQuantity.setScale(4, RoundingMode.HALF_UP))
                .amountBeforeDiscount(money(amountBeforeDiscount))
                .discountAmount(money(discountAmount))
                .totalAmount(totalAmount)
                .taxIncluded(false)
                .filteredCount(filteredCount)
                .filteredAmount(filteredAmount)
                .remark(buildCleanRemark(filteredCount, filteredAmount, barcodeMappingRemark))
                .build();
    }

    public static String formatBarcodeMappingRemark(Map<String, String> barcodeMappings) {
        if (barcodeMappings == null || barcodeMappings.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : barcodeMappings.entrySet()) {
            if (!builder.isEmpty()) {
                builder.append(';');
            }
            builder.append(entry.getKey()).append("->").append(entry.getValue());
        }
        return builder.toString();
    }

    public static String buildCleanRemark(int filteredCount, BigDecimal filteredAmount) {
        return buildCleanRemark(filteredCount, filteredAmount, null);
    }

    public static String buildCleanRemark(int filteredCount, BigDecimal filteredAmount,
                                          String barcodeMappingRemark) {
        String filteredRemark = null;
        if (filteredCount > 0) {
            filteredRemark = String.format("过滤无条码商品 %d 条，金额 %s",
                    filteredCount, filteredAmount.toPlainString());
        }
        if (StrUtil.isBlank(filteredRemark)) {
            return StrUtil.blankToDefault(barcodeMappingRemark, null);
        }
        if (StrUtil.isBlank(barcodeMappingRemark)) {
            return filteredRemark;
        }
        return filteredRemark + "；" + barcodeMappingRemark;
    }

    private static void appendToken(StringBuilder builder, String token) {
        String part = StrUtil.trim(token);
        if (StrUtil.isBlank(part)) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(' ');
        }
        builder.append(part);
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
