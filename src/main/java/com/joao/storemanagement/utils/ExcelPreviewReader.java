package com.joao.storemanagement.utils;

import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.vo.primary.ExcelPreviewVO;
import com.joao.storemanagement.vo.primary.ExcelSheetPreviewVO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ExcelPreviewReader {

    private static final int MAX_PREVIEW_ROWS = 500;
    private static final int MAX_PREVIEW_COLS = 80;

    private ExcelPreviewReader() {
    }

    public static ExcelPreviewVO read(Path path, String filename) {
        if (!isExcel(filename)) {
            throw new BusinessException("当前文件格式暂不支持 Excel 预览");
        }
        try (InputStream inputStream = Files.newInputStream(path);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            DataFormatter formatter = new DataFormatter(Locale.CHINA);
            List<ExcelSheetPreviewVO> sheets = new ArrayList<>();
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                sheets.add(readSheet(workbook.getSheetAt(sheetIndex), formatter));
            }
            return ExcelPreviewVO.builder().sheets(sheets).build();
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException("读取 Excel 预览失败", ex);
        } catch (Exception ex) {
            throw new BusinessException("解析 Excel 预览失败：" + ex.getMessage(), ex);
        }
    }

    private static ExcelSheetPreviewVO readSheet(Sheet sheet, DataFormatter formatter) {
        int lastRow = Math.min(sheet.getLastRowNum(), MAX_PREVIEW_ROWS - 1);
        int maxCol = resolveMaxCol(sheet, lastRow);
        List<List<String>> rows = new ArrayList<>();
        for (int rowIndex = 0; rowIndex <= lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            List<String> values = new ArrayList<>();
            for (int colIndex = 0; colIndex < maxCol; colIndex++) {
                values.add(formatCell(row == null ? null : row.getCell(colIndex), formatter));
            }
            rows.add(values);
        }
        return ExcelSheetPreviewVO.builder()
                .name(sheet.getSheetName())
                .rows(rows)
                .truncated(sheet.getLastRowNum() + 1 > MAX_PREVIEW_ROWS || hasColumnOverflow(sheet, lastRow))
                .build();
    }

    private static int resolveMaxCol(Sheet sheet, int lastRow) {
        int maxCol = 0;
        for (int rowIndex = 0; rowIndex <= lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null && row.getLastCellNum() > maxCol) {
                maxCol = row.getLastCellNum();
            }
        }
        return Math.min(maxCol, MAX_PREVIEW_COLS);
    }

    private static boolean hasColumnOverflow(Sheet sheet, int lastRow) {
        for (int rowIndex = 0; rowIndex <= lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null && row.getLastCellNum() > MAX_PREVIEW_COLS) {
                return true;
            }
        }
        return false;
    }

    private static String formatCell(Cell cell, DataFormatter formatter) {
        return cell == null ? "" : formatter.formatCellValue(cell);
    }

    private static boolean isExcel(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        return lower.endsWith(".xls") || lower.endsWith(".xlsx");
    }
}
