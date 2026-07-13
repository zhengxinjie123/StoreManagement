package com.joao.storemanagement.utils.pricing;

import cn.hutool.core.util.StrUtil;
import com.joao.storemanagement.utils.BarcodeUtil;
import com.joao.storemanagement.utils.ExcelCellReader;
import com.joao.storemanagement.dto.pricing.RetailPriceColumnMappingDTO;
import com.joao.storemanagement.dto.pricing.RetailPriceExcelPreviewDTO;
import com.joao.storemanagement.dto.pricing.RetailPriceMappedLinePreviewDTO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class RetailPriceRefExcelParser {

    public record ParsedRefLine(
            String barcode, String productName, BigDecimal purchasePriceTax, BigDecimal retailPriceTax) {}

    private static final int DEFAULT_SAMPLE_ROWS = 15;
    private static final int DEFAULT_PARSE_PREVIEW_LIMIT = 50;

    private static final String[] PURCHASE_HINTS = {
        "进价含税", "进价", "采购", "进货", "成本", "purchase", "cost", "compra", "custo"
    };

    private static final String[] RETAIL_HINTS = {
        "售价含税", "售价", "零售价", "零售", "销售价", "含税售价", "retail", "pvp", "precio venta", "selling"
    };

    private static final String[] BARCODE_HINTS = {"条码", "barcode", "co.barra", "co barra", "codigo", "código", "ean", "产品条码"};

    private static final String[] NAME_HINTS = {"名称", "name", "descr", "descripcion", "商品", "品名", "product", "产品名称"};

    public RetailPriceExcelPreviewDTO preview(MultipartFile file) {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            return previewSheet(workbook.getSheetAt(0), DEFAULT_SAMPLE_ROWS);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Excel 预览失败: " + ex.getMessage(), ex);
        }
    }

    public List<RetailPriceMappedLinePreviewDTO> parsePreview(
            MultipartFile file, RetailPriceColumnMappingDTO mapping, int limit) {
        return parsePreview(file, mapping, limit, null);
    }

    public List<RetailPriceMappedLinePreviewDTO> parsePreview(
            MultipartFile file, RetailPriceColumnMappingDTO mapping, int limit, String barcodeFilter) {
        validateMapping(mapping);
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int headerRowIdx = findHeaderRow(sheet);
            List<ParsedRefLine> lines = parseRows(sheet, headerRowIdx, mapping, limit, barcodeFilter);
            return lines.stream()
                    .map(l -> new RetailPriceMappedLinePreviewDTO(
                            l.barcode(), l.productName(), l.purchasePriceTax(), l.retailPriceTax()))
                    .toList();
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Excel 解析失败: " + ex.getMessage(), ex);
        }
    }

    public List<ParsedRefLine> parse(MultipartFile file, RetailPriceColumnMappingDTO mapping) {
        validateMapping(mapping);
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int headerRowIdx = findHeaderRow(sheet);
            return parseRows(sheet, headerRowIdx, mapping, Integer.MAX_VALUE, null);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Excel 解析失败: " + ex.getMessage(), ex);
        }
    }

    private RetailPriceExcelPreviewDTO previewSheet(Sheet sheet, int sampleSize) {
        int headerRowIdx = findHeaderRow(sheet);
        Row header = sheet.getRow(headerRowIdx);
        int colCount = columnCount(header);
        List<String> columnLetters = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        for (int c = 0; c < colCount; c++) {
            columnLetters.add(columnLetter(c));
            headers.add(header == null ? "" : StrUtil.trimToEmpty(ExcelCellReader.cell(header, c)));
        }
        List<List<String>> sampleRows = new ArrayList<>();
        int end = Math.min(sheet.getLastRowNum(), headerRowIdx + sampleSize);
        for (int i = headerRowIdx + 1; i <= end; i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            List<String> cells = new ArrayList<>();
            for (int c = 0; c < colCount; c++) {
                cells.add(ExcelCellReader.cellPlain(row, c));
            }
            sampleRows.add(cells);
        }
        int totalDataRows = Math.max(0, sheet.getLastRowNum() - headerRowIdx);
        RetailPriceColumnMappingDTO suggested = suggestMapping(headers);
        return new RetailPriceExcelPreviewDTO(headerRowIdx, columnLetters, headers, sampleRows, totalDataRows, suggested);
    }

    private List<ParsedRefLine> parseRows(
            Sheet sheet, int headerRowIdx, RetailPriceColumnMappingDTO mapping, int limit, String barcodeFilter) {
        String normalizedFilter =
                StrUtil.isNotBlank(barcodeFilter) ? BarcodeUtil.judgeBarcode(barcodeFilter) : null;
        boolean searchBarcode = StrUtil.isNotBlank(normalizedFilter);
        List<ParsedRefLine> lines = new ArrayList<>();
        for (int i = headerRowIdx + 1; i <= sheet.getLastRowNum(); i++) {
            if (!searchBarcode && lines.size() >= limit) {
                break;
            }
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            ParsedRefLine line = parseRow(row, mapping);
            if (line == null) {
                continue;
            }
            if (searchBarcode) {
                if (normalizedFilter.equals(BarcodeUtil.judgeBarcode(line.barcode()))) {
                    lines.add(line);
                    break;
                }
                continue;
            }
            lines.add(line);
        }
        if (lines.isEmpty() && !searchBarcode) {
            throw new IllegalArgumentException("未解析到有效的条码与售价行，请检查列映射");
        }
        return lines;
    }

    private ParsedRefLine parseRow(Row row, RetailPriceColumnMappingDTO mapping) {
        String rawBarcode = ExcelCellReader.cell(row, mapping.barcodeCol());
        BigDecimal retailPriceTax = ExcelCellReader.decimal(row, mapping.retailPriceCol());
        if (StrUtil.isAllBlank(rawBarcode) && retailPriceTax == null) {
            return null;
        }
        String barcode = BarcodeUtil.judgeBarcode(rawBarcode);
        if (StrUtil.isBlank(barcode)) {
            return null;
        }
        if (retailPriceTax == null || retailPriceTax.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        String name = mapping.nameCol() != null ? StrUtil.trimToNull(ExcelCellReader.cell(row, mapping.nameCol())) : null;
        BigDecimal purchasePriceTax = null;
        if (mapping.purchasePriceCol() != null) {
            purchasePriceTax = ExcelCellReader.decimal(row, mapping.purchasePriceCol());
            if (purchasePriceTax != null && purchasePriceTax.compareTo(BigDecimal.ZERO) <= 0) {
                purchasePriceTax = null;
            }
        }
        return new ParsedRefLine(barcode, name, purchasePriceTax, retailPriceTax);
    }

    private void validateMapping(RetailPriceColumnMappingDTO mapping) {
        if (mapping.barcodeCol() == null || mapping.barcodeCol() < 0) {
            throw new IllegalArgumentException("请指定条码列");
        }
        if (mapping.retailPriceCol() == null || mapping.retailPriceCol() < 0) {
            throw new IllegalArgumentException("请指定售价（含税）列");
        }
        if (mapping.barcodeCol().equals(mapping.retailPriceCol())) {
            throw new IllegalArgumentException("条码列与售价列不能相同");
        }
        if (mapping.purchasePriceCol() != null
                && (mapping.purchasePriceCol().equals(mapping.barcodeCol())
                        || mapping.purchasePriceCol().equals(mapping.retailPriceCol()))) {
            throw new IllegalArgumentException("进价列不能与条码列或售价列相同");
        }
    }

    private RetailPriceColumnMappingDTO suggestMapping(List<String> headers) {
        int barcodeCol = findColumnIndex(headers, BARCODE_HINTS, null);
        int nameCol = findColumnIndex(headers, NAME_HINTS, null);
        int purchaseCol = findColumnIndex(headers, PURCHASE_HINTS, RETAIL_HINTS);
        int retailCol = findRetailColumnIndex(headers);
        if (barcodeCol < 0) {
            barcodeCol = 0;
        }
        if (retailCol < 0) {
            if (purchaseCol >= 0 && purchaseCol + 1 < headers.size()) {
                retailCol = purchaseCol + 1;
            } else {
                retailCol = firstUnusedColumn(headers.size(), barcodeCol, nameCol, purchaseCol);
                if (retailCol < 0) {
                    retailCol = barcodeCol == 0 ? 1 : 0;
                }
            }
        }
        if (purchaseCol >= 0 && retailCol == purchaseCol && purchaseCol + 1 < headers.size()) {
            retailCol = purchaseCol + 1;
        }
        if (retailCol == barcodeCol) {
            retailCol = firstUnusedColumn(headers.size(), barcodeCol, nameCol, purchaseCol, retailCol);
            if (retailCol < 0 && headers.size() > 1) {
                retailCol = barcodeCol == 0 ? 1 : 0;
            }
        }
        if (nameCol >= 0 && (nameCol == barcodeCol || nameCol == purchaseCol || nameCol == retailCol)) {
            nameCol = -1;
        }
        return new RetailPriceColumnMappingDTO(
                barcodeCol, nameCol >= 0 ? nameCol : null, purchaseCol >= 0 ? purchaseCol : null, retailCol);
    }

    private int firstUnusedColumn(int colCount, int... usedCols) {
        java.util.Set<Integer> used = new java.util.HashSet<>();
        for (int col : usedCols) {
            if (col >= 0) {
                used.add(col);
            }
        }
        for (int c = colCount - 1; c >= 0; c--) {
            if (!used.contains(c)) {
                return c;
            }
        }
        return -1;
    }

    private int findRetailColumnIndex(List<String> headers) {
        int explicit = findColumnIndex(headers, RETAIL_HINTS, PURCHASE_HINTS);
        if (explicit >= 0) {
            return explicit;
        }
        for (int c = 0; c < headers.size(); c++) {
            String text = normalizeHeader(headers.get(c));
            if (text.contains("鍚◣") && !containsAny(text, PURCHASE_HINTS)) {
                return c;
            }
        }
        return -1;
    }

    private int findColumnIndex(List<String> headers, String[] includeHints, String[] excludeHints) {
        for (int c = 0; c < headers.size(); c++) {
            String text = normalizeHeader(headers.get(c));
            if (excludeHints != null && containsAny(text, excludeHints)) {
                continue;
            }
            if (containsAny(text, includeHints)) {
                return c;
            }
        }
        return -1;
    }

    private int findHeaderRow(Sheet sheet) {
        for (int i = 0; i <= Math.min(5, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            List<String> headers = new ArrayList<>();
            for (int c = 0; c < columnCount(row); c++) {
                headers.add(ExcelCellReader.cell(row, c));
            }
            if (looksLikeHeaderRow(headers)) {
                return i;
            }
        }
        return 0;
    }

    private boolean looksLikeHeaderRow(List<String> cells) {
        if (findColumnIndex(cells, BARCODE_HINTS, null) >= 0
                || findRetailColumnIndex(cells) >= 0
                || findColumnIndex(cells, PURCHASE_HINTS, RETAIL_HINTS) >= 0) {
            return true;
        }
        String first = cells.isEmpty() ? "" : StrUtil.trimToEmpty(cells.get(0));
        if (first.matches("\\d{8,}")) {
            return false;
        }
        int textCells = 0;
        for (String cell : cells) {
            String value = StrUtil.trimToEmpty(cell);
            if (value.isEmpty()) {
                continue;
            }
            if (!value.matches("^-?\\d+([.,]\\d+)?$")) {
                textCells++;
            }
        }
        return textCells >= 2;
    }

    private int columnCount(Row row) {
        if (row == null) {
            return 0;
        }
        return Math.max(0, row.getLastCellNum());
    }

    private String columnLetter(int index) {
        int n = index;
        StringBuilder sb = new StringBuilder();
        do {
            sb.insert(0, (char) ('A' + (n % 26)));
            n = n / 26 - 1;
        } while (n >= 0);
        return sb.toString();
    }

    private String normalizeHeader(String raw) {
        return StrUtil.trimToEmpty(raw).toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String text, String[] hints) {
        for (String hint : hints) {
            if (text.contains(hint.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}


