package com.joao.storemanagement.category.invoiceclean;

import cn.hutool.core.util.StrUtil;
import com.joao.storemanagement.entity.primary.InvoiceTemplate;
import com.joao.storemanagement.utils.ExcelCellReader;
import com.joao.storemanagement.vo.primary.InvoiceCleanSummaryVO;
import org.apache.poi.ss.usermodel.Sheet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class InvoiceCleanSupport {

    public static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("23");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final Pattern TOTAL_PATTERN = Pattern.compile("Total:\\s*([\\d,.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CJK_PATTERN = Pattern.compile("[\\u4e00-\\u9fff\\u3400-\\u4dbf\\uff00-\\uffef]+");

    private InvoiceCleanSupport() {
    }

    public static boolean hasTaxRateColumn(InvoiceTemplate template) {
        return StrUtil.isNotBlank(template.getTaxRateCol());
    }

    public static String joinName(String current, String extra) {
        return StrUtil.isBlank(current) ? extra.trim() : current + " " + extra.trim();
    }

    public static String normalizeProductName(String name) {
        if (StrUtil.isBlank(name)) {
            return StrUtil.nullToEmpty(name);
        }
        return name.trim().toUpperCase(Locale.ROOT);
    }

    public static NameParts normalizeNameParts(NameParts nameParts) {
        if (nameParts == null) {
            return new NameParts("", "");
        }
        return new NameParts(
                normalizeProductName(nameParts.chinese()),
                normalizeProductName(nameParts.foreignName()));
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

    public static BigDecimal readTaxRateFromColumn(Sheet sheet, int rowIndex, InvoiceTemplate template) {
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
        String chinese = ExcelCellReader.readString(sheet, rowIndex, template.getChineseNameCol());
        return normalizeNameParts(new NameParts(StrUtil.trim(chinese), StrUtil.trim(rawName)));
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
            return normalizeNameParts(new NameParts("", trimmed));
        }
        return normalizeNameParts(new NameParts(chinese.toString().trim(), foreign.toString().trim()));
    }

    public static void appendName(List<InvoiceCleanRow> rows, NameParts nameParts, Sheet sheet, int rowIndex, InvoiceTemplate template) {
        if (rows.isEmpty()) {
            return;
        }
        String chinese = nameParts.chinese();
        String foreign = getStr(nameParts);
        if (StrUtil.isEmpty(chinese) && StrUtil.isEmpty(foreign)) {
            return;
        }
        String lastRowBarcode = ExcelCellReader.readString(sheet, rowIndex - 1, template.getBarcodeCol());
        // 如果上一行没有条码, 则不合并
        if (StrUtil.isEmpty(lastRowBarcode)) {
            return;
        }

        InvoiceCleanRow last = rows.get(rows.size() - 1);
        if (StrUtil.isNotEmpty(chinese)) {
            // 这里的名称如果是数字类型需要二次处理
            if (chinese.matches("^-?\\d+\\.0+$")) {
                chinese = chinese.substring(0, chinese.indexOf('.'));
            }
            last.setChineseName(normalizeProductName(joinName(last.getChineseName(), chinese)));
        }
        if (StrUtil.isNotEmpty(foreign)) {
            if (foreign.matches("^-?\\d+\\.0+$")) {
                foreign = foreign.substring(0, foreign.indexOf('.'));
            }
            last.setForeignName(normalizeProductName(joinName(last.getForeignName(), foreign)));
        }
    }

    private static String getStr(NameParts nameParts) {
        return nameParts.foreignName();
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
        return buildSummary(rows, taxIncluded, footerTotal, null, filteredCount, filteredAmount, null, null);
    }

    public static InvoiceCleanSummaryVO buildSummary(List<InvoiceCleanRow> rows, boolean taxIncluded,
                                                     BigDecimal footerTotal,
                                                     InvoiceFooterSummary invoiceFooter,
                                                     int filteredCount,
                                                     BigDecimal filteredAmount) {
        return buildSummary(rows, taxIncluded, footerTotal, invoiceFooter, filteredCount, filteredAmount, null, null);
    }

    public static InvoiceCleanSummaryVO buildSummary(List<InvoiceCleanRow> rows, boolean taxIncluded,
                                                     BigDecimal footerTotal,
                                                     InvoiceFooterSummary invoiceFooter,
                                                     int filteredCount,
                                                     BigDecimal filteredAmount,
                                                     String barcodeMappingRemark,
                                                     Set<Integer> rowIndexSet) {
        if (invoiceFooter != null && invoiceFooter.hasInvoiceLevelDiscount()) {
            return buildInvoiceFooterSummary(rows, taxIncluded, invoiceFooter, filteredCount, filteredAmount,
                    barcodeMappingRemark, rowIndexSet);
        }
        if (taxIncluded) {
            return buildTaxIncludedSummary(rows, footerTotal, filteredCount, filteredAmount, barcodeMappingRemark, rowIndexSet);
        }
        return buildTaxExcludedSummary(rows, footerTotal, filteredCount, filteredAmount, barcodeMappingRemark, rowIndexSet);
    }

    private static InvoiceCleanSummaryVO buildInvoiceFooterSummary(List<InvoiceCleanRow> rows,
                                                                   boolean taxIncluded,
                                                                   InvoiceFooterSummary invoiceFooter,
                                                                   int filteredCount,
                                                                   BigDecimal filteredAmount,
                                                                   String barcodeMappingRemark,
                                                                   Set<Integer> rowIndexSet) {
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
                .remark(buildCleanRemark(filteredCount, filteredAmount, barcodeMappingRemark, rowIndexSet))
                .build();
    }

    private static InvoiceCleanSummaryVO buildTaxIncludedSummary(List<InvoiceCleanRow> rows,
                                                                 BigDecimal footerTotal,
                                                                 int filteredCount,
                                                                 BigDecimal filteredAmount,
                                                                 String barcodeMappingRemark,
                                                                 Set<Integer> rowIndexSet) {
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
                .remark(buildCleanRemark(filteredCount, filteredAmount, barcodeMappingRemark, rowIndexSet))
                .build();
    }

    private static InvoiceCleanSummaryVO buildTaxExcludedSummary(List<InvoiceCleanRow> rows,
                                                                 BigDecimal footerTotal,
                                                                 int filteredCount,
                                                                 BigDecimal filteredAmount,
                                                                 String barcodeMappingRemark,
                                                                 Set<Integer> rowIndexSet) {
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
                .remark(buildCleanRemark(filteredCount, filteredAmount, barcodeMappingRemark, rowIndexSet))
                .build();
    }

    /**
     * 按表格明细行计算折前金额（与汇总口径一致）：含税模式累加含税单价×数量，否则累加不含税单价×数量。
     * 用于与页脚解析出的折前金额做一致性校验。
     */
    public static BigDecimal calcAmountBeforeDiscount(List<InvoiceCleanRow> rows, boolean taxIncluded) {
        BigDecimal amount = BigDecimal.ZERO;
        for (InvoiceCleanRow row : rows) {
            BigDecimal price = taxIncluded ? row.getUnitPriceIncTax() : row.getUnitPriceExTax();
            if (price == null || row.getQuantity() == null) {
                continue;
            }
            amount = amount.add(price.multiply(row.getQuantity()));
        }
        return money(amount);
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
        return buildCleanRemark(filteredCount, filteredAmount, null, null);
    }

    public static String buildCleanRemark(int filteredCount, BigDecimal filteredAmount,
                                          String barcodeMappingRemark, Set<Integer> rowIndexSet) {
        StringBuilder sb = new StringBuilder();
        if (filteredCount > 0) {
            sb.append(String.format("过滤无效商品 %d 条，金额 %s；",
                    filteredCount, filteredAmount.toPlainString()))
                    .append("具体原发票所在行数如下: ")
                    .append(
                    rowIndexSet.stream()
                            .sorted()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", "))
            );
        }

        return sb.append(StrUtil.blankToDefault(barcodeMappingRemark, "")).toString();
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

    public record NameParts(String chinese, String foreignName) {
    }
}
