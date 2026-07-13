package com.joao.storemanagement.utils;

import cn.hutool.core.util.StrUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExcelCellReader {

    private static final Pattern DECIMAL_TOKEN = Pattern.compile("(-?[\\d.,]+)");
    private static final DataFormatter FORMATTER = new DataFormatter();

    private ExcelCellReader() {
    }

    public static int columnIndex(String column) {
        String normalized = column.trim().toUpperCase();
        int index = 0;
        for (int i = 0; i < normalized.length(); i++) {
            index = index * 26 + (normalized.charAt(i) - 'A' + 1);
        }
        return index - 1;
    }

    public static String readString(Sheet sheet, int rowIndex, String column) {
        Cell cell = getCell(sheet, rowIndex, column);
        if (cell == null) {
            return "";
        }
        return formatInvoiceCell(cell).trim();
    }

    public static BigDecimal readDecimal(Sheet sheet, int rowIndex, String column) {
        return readDecimal(sheet, rowIndex, column, false);
    }

    public static BigDecimal readDecimal(Sheet sheet, int rowIndex, String column, boolean stripCurrency) {
        return parseDecimal(readString(sheet, rowIndex, column), stripCurrency);
    }

    public static BigDecimal parseDecimal(String text, boolean stripCurrency) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.trim();
        if (stripCurrency) {
            Matcher matcher = DECIMAL_TOKEN.matcher(normalized);
            if (!matcher.find()) {
                return null;
            }
            normalized = matcher.group(1);
        }
        normalized = normalized.replace("%", "").trim();
        return parseLocaleNumber(normalized);
    }

    public static String cell(Row row, int index) {
        if (row == null) {
            return "";
        }
        return FORMATTER.formatCellValue(row.getCell(index)).trim();
    }

    public static String cellPlain(Row row, int index) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(index);
        if (cell == null) {
            return "";
        }
        BigDecimal decimal = decimal(cell);
        if (decimal != null) {
            int scale = Math.max(2, decimal.stripTrailingZeros().scale());
            return decimal.setScale(Math.min(scale, 4), RoundingMode.HALF_UP).toPlainString();
        }
        return FORMATTER.formatCellValue(cell).trim();
    }

    public static BigDecimal decimal(Row row, int index) {
        if (row == null) {
            return null;
        }
        return decimal(row.getCell(index));
    }

    public static BigDecimal decimal(Cell cell) {
        if (cell == null) {
            return null;
        }
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        if (type == CellType.NUMERIC) {
            return decimalFromNumericCell(cell);
        }
        if (type == CellType.STRING) {
            return normalizePrice(parseDecimalString(cell.getStringCellValue()));
        }
        if (type == CellType.BLANK) {
            return null;
        }
        return normalizePrice(parseDecimalString(FORMATTER.formatCellValue(cell)));
    }

    public static boolean rowContains(Row row, String... keywords) {
        if (row == null) {
            return false;
        }
        for (int i = 0; i < 20; i++) {
            String value = cell(row, i);
            if (value.isEmpty()) {
                continue;
            }
            for (String keyword : keywords) {
                if (value.contains(keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal parseLocaleNumber(String value) {
        if (value.isBlank()) {
            return null;
        }
        int lastComma = value.lastIndexOf(',');
        int lastDot = value.lastIndexOf('.');
        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                value = value.replace(".", "").replace(',', '.');
            } else {
                value = value.replace(",", "");
            }
        } else if (lastComma >= 0) {
            value = value.replace(',', '.');
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Cell getCell(Sheet sheet, int rowIndex, String column) {
        if (sheet.getRow(rowIndex) == null) {
            return null;
        }
        return sheet.getRow(rowIndex).getCell(columnIndex(column));
    }

    private static String formatInvoiceCell(Cell cell) {
        return switch (cell.getCellType()) {
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield FORMATTER.formatCellValue(cell);
                }
                double value = cell.getNumericCellValue();
                if (Double.isFinite(value) && value == Math.rint(value)) {
                    yield BigDecimal.valueOf(value).toPlainString();
                }
                yield BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> formatFormulaCell(cell);
            default -> FORMATTER.formatCellValue(cell);
        };
    }

    private static String formatFormulaCell(Cell cell) {
        try {
            if (cell.getCachedFormulaResultType() == CellType.NUMERIC) {
                double value = cell.getNumericCellValue();
                if (Double.isFinite(value) && value == Math.rint(value)) {
                    return BigDecimal.valueOf(value).toPlainString();
                }
                return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return FORMATTER.formatCellValue(cell);
    }

    private static BigDecimal decimalFromNumericCell(Cell cell) {
        double rawValue = cell.getNumericCellValue();
        BigDecimal fromRaw = new BigDecimal(Double.toString(rawValue));
        if (fromRaw.stripTrailingZeros().scale() > 0) {
            return normalizePrice(fromRaw);
        }
        BigDecimal fromDisplay = parseDecimalString(FORMATTER.formatCellValue(cell));
        if (fromDisplay != null && fromDisplay.stripTrailingZeros().scale() > fromRaw.stripTrailingZeros().scale()) {
            return normalizePrice(fromDisplay);
        }
        return normalizePrice(fromRaw);
    }

    private static BigDecimal normalizePrice(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private static BigDecimal parseDecimalString(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String cleaned = raw.replace("€", "").replace("\u00a0", "").trim();
        if (cleaned.matches(".*,\\d+$") && cleaned.contains(".")) {
            cleaned = cleaned.replace(".", "").replace(",", ".");
        } else {
            cleaned = cleaned.replace(",", ".");
        }
        try {
            if (cleaned.contains("E") || cleaned.contains("e")) {
                return new BigDecimal(Double.toString(Double.parseDouble(cleaned)));
            }
            return new BigDecimal(cleaned);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
