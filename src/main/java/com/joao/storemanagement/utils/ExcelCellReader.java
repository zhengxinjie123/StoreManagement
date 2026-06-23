package com.joao.storemanagement.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
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
        return formatCell(cell).trim();
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

    private static String formatCell(Cell cell) {
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
            if (cell.getCachedFormulaResultType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
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

    public static BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
