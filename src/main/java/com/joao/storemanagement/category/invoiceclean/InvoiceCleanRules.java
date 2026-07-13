package com.joao.storemanagement.category.invoiceclean;

import cn.hutool.core.util.StrUtil;
import com.joao.storemanagement.entity.primary.InvoiceTemplate;
import com.joao.storemanagement.utils.ExcelCellReader;
import org.apache.poi.ss.usermodel.Sheet;

import java.math.BigDecimal;
import java.util.*;

import static com.joao.storemanagement.category.invoiceclean.InvoiceCleanSupport.*;

/**
 * 发票明细行解析规则组合器，供各供应商清洗策略复用。
 * <p>
 * 统一清洗流程由 {@link com.joao.storemanagement.serviceImpl.primary.InvoiceCleanServiceImpl} 控制，供应商差异通过
 * {@link InvoiceCleanStrategyFactory} 下发至各 {@link InvoiceCleanStrategy} 实现；
 * 各策略持有不同的 {@link InvoiceCleanOptions}，在本类中组合同一套行级解析流程。
 * <p>
 * 默认策略使用 {@link InvoiceCleanOptions#standard()}；特殊规则（过滤无条码行、拆分混合品名、
 * 仅读列税率、Subtotal 停扫等）由 options 开关控制，无需为每个供应商复制解析代码。
 */
public final class InvoiceCleanRules {

    private InvoiceCleanRules() {
    }

    /**
     * 按模板列配置与 options 规则，逐行解析 Excel 明细并产出清洗行。
     *
     * @param sheet       发票工作表
     * @param template    供应商清洗模板（列映射、起始行、默认税率等）
     * @param taxIncluded 模板是否含税价模式
     * @param options     供应商可选规则组合
     * @return 明细行列表及被过滤行的数量/金额统计
     */
    public static InvoiceParseResult parse(Sheet sheet, InvoiceTemplate template, boolean taxIncluded,
                                           InvoiceCleanOptions options) {
        List<InvoiceCleanRow> rows = new ArrayList<>();
        List<InvoiceFilteredRow> filteredRows = new ArrayList<>();
        int filteredCount = 0;
        BigDecimal filteredAmount = BigDecimal.ZERO;
        Set<Integer> rowIndexSet = new TreeSet<>();
        // 原条码 -> 新条码，按发票行顺序保留，同一原条码只记录首次映射
        Map<String, String> barcodeMappings = new LinkedHashMap<>();
        // 模板 dataStartRow 为 1-based，POI 行索引为 0-based
        int startRow = template.getDataStartRow() - 1;
        int lastRow = sheet.getLastRowNum();

        for (int rowIndex = startRow; rowIndex <= lastRow; rowIndex++) {
            // 校验当前行是否还是数据行
            if (shouldBreakAtRow(sheet, rowIndex, template, options)) {
                break;
            }

            // 读取条码列，作为有效商品行的首要判定依据
            String barcode = ExcelCellReader.readString(sheet, rowIndex, template.getBarcodeCol());

            // 解析中/外文品名（独立列或混合列拆分）
            InvoiceCleanSupport.NameParts nameParts = resolveNameParts(sheet, rowIndex, template, options);

            // 中英文品名均为空时跳过
            if (StrUtil.isEmpty(nameParts.chinese()) && StrUtil.isEmpty(nameParts.foreignName())) {
                continue;
            }

            // 如果没有条码
            if (StrUtil.isEmpty(barcode)) {
                // 当前供应商是否配置了,无条码但是有商品数量的过滤配置
                if (options.isFilterRowsWithoutBarcode()) {
                    // 无条码但有数量的行视为无法导入的商品，计入过滤统计
                    BigDecimal quantity = ExcelCellReader.readDecimal(sheet, rowIndex, template.getQuantityCol());
                    if (quantity == null) {
                        // 如果没有条码也没有数量, 可能是上一行商品的商品名过长导致换行, 这里合并商品名
                        appendName(rows, nameParts, sheet, rowIndex, template);
                        continue;
                    }
                    filteredCount++;
                    // 估算被过滤行的含税金额，供汇总备注使用
                    BigDecimal amount = estimateLineAmount(sheet, rowIndex, template, taxIncluded, quantity, options);
                    filteredAmount = filteredAmount.add(amount);
                    rowIndexSet.add(rowIndex + 1);
                    filteredRows.add(buildFilteredRow(sheet, rowIndex, template, options, taxIncluded,
                            "", nameParts, quantity, amount, InvoiceFilteredRow.REASON_NO_BARCODE));
                    continue;
                }
                // 不过滤时，将无条码续行品名合并到上一行
                appendName(rows, nameParts, sheet, rowIndex, template);
                continue;
            }
            BigDecimal quantity = ExcelCellReader.readDecimal(sheet, rowIndex, template.getQuantityCol());
            // 有条码但是没有数量视为非商品行
            if (quantity == null) {
                appendName(rows, nameParts, sheet, rowIndex, template);
                continue;
            }

            // 条码不为空, 并且开启了过滤非ean13条码的供应商, 会将这些条码过滤记入备注
            if (options.isSkipBarcodeNotEAN13()) {
                if (barcode.length() != 13) {
                    filteredCount++;
                    BigDecimal amount = estimateLineAmount(sheet, rowIndex, template, taxIncluded, quantity, options);
                    filteredAmount = filteredAmount.add(amount);
                    rowIndexSet.add(rowIndex + 1);
                    filteredRows.add(buildFilteredRow(sheet, rowIndex, template, options, taxIncluded,
                            StrUtil.trim(barcode), nameParts, quantity, amount, InvoiceFilteredRow.REASON_NON_EAN13));
                    continue;
                }
            }



            // 解析行税率：优先列值，否则模板默认或系统默认
            BigDecimal taxRate = resolveTaxRate(sheet, rowIndex, template, options);

            // 分别读取不含税/含税单价，缺失一侧由另一侧反算补齐
            BigDecimal unitPriceExTax = readUnitPriceExTax(sheet, rowIndex, template, options);
            BigDecimal unitPriceIncTax = readUnitPriceIncTax(sheet, rowIndex, template, options);
            if (unitPriceExTax == null && unitPriceIncTax == null) {
                continue;
            }
            if (unitPriceExTax == null) {
                unitPriceExTax = calcTaxExcludedPrice(unitPriceIncTax, taxRate);
            }
            if (unitPriceIncTax == null) {
                unitPriceIncTax = calcTaxIncludedPrice(unitPriceExTax, taxRate);
            }

            // 跳过价格为 0 的 PALLET 托盘占位行
            if (options.isSkipZeroPricePalletRows()
                    && isZeroPricePalletRow(nameParts, unitPriceExTax, unitPriceIncTax, taxIncluded)) {
                continue;
            }

            // 优先读行小计列；无小计列时用单价×数量计算
            BigDecimal lineSubtotalFromCol = readLineSubtotal(sheet, rowIndex, template, options);
            BigDecimal lineSubtotalExTax;
            BigDecimal lineSubtotalIncTax;
            if (lineSubtotalFromCol != null) {
                if (lineSubtotalColumnIsTaxIncluded(taxIncluded)) {
                    lineSubtotalIncTax = lineSubtotalFromCol;
                    lineSubtotalExTax = calcTaxExcludedPrice(lineSubtotalIncTax, taxRate);
                } else {
                    lineSubtotalExTax = lineSubtotalFromCol;
                    lineSubtotalIncTax = calcLineTotalIncTax(lineSubtotalExTax, taxRate);
                }
            } else {
                lineSubtotalExTax = unitPriceExTax.multiply(quantity);
                lineSubtotalIncTax = unitPriceIncTax.multiply(quantity);
            }
            // 输出进价列跟随模板含税/不含税模式
            BigDecimal outputPrice = taxIncluded ? unitPriceIncTax : unitPriceExTax;

            InvoiceCleanRow row = new InvoiceCleanRow();
            row.setSourceRowIndex(rowIndex + 1);
            row.setBarcode(StrUtil.trim(barcode));
            row.setChineseName(InvoiceCleanSupport.normalizeProductName(nameParts.chinese()));
            row.setForeignName(InvoiceCleanSupport.normalizeProductName(nameParts.foreignName()));
            row.setQuantity(quantity);
            row.setUnitPriceExTax(unitPriceExTax);
            row.setUnitPriceIncTax(unitPriceIncTax);
            row.setOutputPrice(outputPrice);
            row.setTaxRate(taxRate);
            row.setLineSubtotalExTax(lineSubtotalExTax);
            row.setLineSubtotalIncTax(lineSubtotalIncTax);
            rows.add(row);
            // 爱国者等双条码发票：记录原条码与新条码对应关系，写入清洗备注
            collectBarcodeMapping(sheet, rowIndex, template, options, StrUtil.trim(barcode), barcodeMappings);
        }

        return InvoiceParseResult.builder()
                .rows(rows)
                .filteredCount(filteredCount)
                .filteredAmount(money(filteredAmount))
                .rowIndexSet(rowIndexSet)
                .barcodeMappingRemark(InvoiceCleanSupport.formatBarcodeMappingRemark(barcodeMappings))
                .filteredRows(filteredRows)
                .build();
    }

    /**
     * 解析当前行的中/外文品名。
     * 模板配置了中文名列时走标准双列读取；否则按 options 决定是否从外文列拆分混合品名。
     */
    private static InvoiceCleanSupport.NameParts resolveNameParts(Sheet sheet, int rowIndex,
                                                                  InvoiceTemplate template,
                                                                  InvoiceCleanOptions options) {
        if (StrUtil.isNotEmpty(template.getChineseNameCol())) {
            return resolveName(sheet, rowIndex, template);
        }
        String rawName = ExcelCellReader.readString(sheet, rowIndex, template.getForeignNameCol());
        if (options.isSplitMixedChineseForeignName()) {
            // 从同一列文本中拆分中文与外文片段
            return splitMixedName(rawName);
        }
        return new InvoiceCleanSupport.NameParts("", InvoiceCleanSupport.normalizeProductName(StrUtil.trim(rawName)));
    }

    /**
     * 判断行小计列是否表示含税金额。
     * 行小计跟随模板含税模式：含税入库时小计列按含税金额解析，不含税入库时按不含税金额解析。
     */
    static boolean lineSubtotalColumnIsTaxIncluded(boolean taxIncluded) {
        return taxIncluded;
    }

    /**
     * 解析行税率：如果模版配置了税率列, 优先使用, 如果当前行税率是空, 则赋值默认税率;
     * 如果没有配置税率列, 则赋值默认税率。
     */
    private static BigDecimal resolveTaxRate(Sheet sheet, int rowIndex, InvoiceTemplate template,
                                             InvoiceCleanOptions options) {

        if (hasTaxRateColumn(template)) {
            BigDecimal bigDecimal = readTaxRateFromColumn(sheet, rowIndex, template);
            return bigDecimal == null ? DEFAULT_TAX_RATE : bigDecimal;
        }
        return DEFAULT_TAX_RATE;
    }

    /**
     * 构建预览用过滤行。过滤行虽然不会直接进入清洗结果，但前端需要基于这些默认值修正后再归档。
     */
    private static InvoiceFilteredRow buildFilteredRow(Sheet sheet, int rowIndex, InvoiceTemplate template,
                                                       InvoiceCleanOptions options, boolean taxIncluded,
                                                       String barcode, InvoiceCleanSupport.NameParts nameParts,
                                                       BigDecimal quantity, BigDecimal amount, String reason) {
        BigDecimal taxRate = resolveTaxRate(sheet, rowIndex, template, options);
        BigDecimal unitPriceExTax = readUnitPriceExTax(sheet, rowIndex, template, options);
        BigDecimal unitPriceIncTax = readUnitPriceIncTax(sheet, rowIndex, template, options);
        if (unitPriceExTax == null && unitPriceIncTax != null && taxRate != null) {
            unitPriceExTax = calcTaxExcludedPrice(unitPriceIncTax, taxRate);
        }
        if (unitPriceIncTax == null && unitPriceExTax != null && taxRate != null) {
            unitPriceIncTax = calcTaxIncludedPrice(unitPriceExTax, taxRate);
        }
        BigDecimal outputPrice = taxIncluded ? unitPriceIncTax : unitPriceExTax;
        if (outputPrice == null && quantity != null && quantity.compareTo(BigDecimal.ZERO) != 0) {
            outputPrice = amount.divide(quantity, 4, java.math.RoundingMode.HALF_UP);
        }
        return InvoiceFilteredRow.builder()
                .sourceRowIndex(rowIndex + 1)
                .barcode(barcode)
                .chineseName(InvoiceCleanSupport.normalizeProductName(nameParts.chinese()))
                .foreignName(InvoiceCleanSupport.normalizeProductName(nameParts.foreignName()))
                .quantity(quantity)
                .outputPrice(outputPrice)
                .taxRate(taxRate)
                .amount(amount)
                .reason(reason)
                .build();
    }

    /**
     * 估算被过滤行（无条码但有数量）的含税行金额，用于过滤统计。
     */
    private static BigDecimal estimateLineAmount(Sheet sheet, int rowIndex, InvoiceTemplate template,
                                                 boolean taxIncluded, BigDecimal quantity,
                                                 InvoiceCleanOptions options) {
        BigDecimal taxRate = resolveTaxRate(sheet, rowIndex, template, options);

        // 有小计列时优先用小计估算
        BigDecimal lineSubtotalExTax = readLineSubtotal(sheet, rowIndex, template, options);
        if (lineSubtotalExTax != null) {
            if (lineSubtotalColumnIsTaxIncluded(taxIncluded)) {
                return money(lineSubtotalExTax);
            }
            if (taxRate != null) {
                return money(calcLineTotalIncTax(lineSubtotalExTax, taxRate));
            }
            return money(lineSubtotalExTax);
        }

        // 无小计列时用单价×数量估算
        BigDecimal unitPriceExTax = readUnitPriceExTax(sheet, rowIndex, template, options);
        BigDecimal unitPriceIncTax = readUnitPriceIncTax(sheet, rowIndex, template, options);
        if (unitPriceExTax == null && unitPriceIncTax == null) {
            return BigDecimal.ZERO;
        }
        if (unitPriceExTax == null && unitPriceIncTax != null && taxRate != null) {
            unitPriceExTax = calcTaxExcludedPrice(unitPriceIncTax, taxRate);
        }
        if (unitPriceIncTax == null && unitPriceExTax != null && taxRate != null) {
            unitPriceIncTax = calcTaxIncludedPrice(unitPriceExTax, taxRate);
        }
        if (taxIncluded && unitPriceIncTax != null) {
            return money(unitPriceIncTax.multiply(quantity));
        }
        if (unitPriceExTax != null && taxRate != null) {
            return money(calcLineTotalIncTax(unitPriceExTax.multiply(quantity), taxRate));
        }
        if (unitPriceExTax != null) {
            return money(unitPriceExTax.multiply(quantity));
        }
        return BigDecimal.ZERO;
    }

    /**
     * 判断当前行是否是数据行, 需要供应商配置开启页脚检测
     * 如果当前行的20列之内存在Base Incidencia 则视为页脚, 结束数据行读取
     */
    private static boolean shouldBreakAtRow(Sheet sheet, int rowIndex, InvoiceTemplate template,
                                           InvoiceCleanOptions options) {
        if (options.isBreakOnTaxableBase()) {
            // 小计标签可能出现在条码列以外的任意列
            for (int col = 0; col <= 20; col++) {
                String text = ExcelCellReader.readString(sheet, rowIndex, columnLetter(col));
                if (StrUtil.equalsIgnoreCase(StrUtil.trim(text), "Base Incidencia")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 读取新条码列并记录原条码与新条码的对应关系。
     * 仅在 options 开启且模板配置了 newBarcodeCol 时生效；新条码为空或与原条码相同时跳过。
     */
    private static void collectBarcodeMapping(Sheet sheet, int rowIndex, InvoiceTemplate template,
                                              InvoiceCleanOptions options, String oldBarcode,
                                              Map<String, String> barcodeMappings) {
        if (!options.isProductHasNewBarcode() || StrUtil.isBlank(template.getNewBarcodeCol())) {
            return;
        }
        String newBarcode = StrUtil.trim(ExcelCellReader.readString(sheet, rowIndex, template.getNewBarcodeCol()));
        if (StrUtil.isBlank(newBarcode) || StrUtil.equals(oldBarcode, newBarcode)) {
            return;
        }
        barcodeMappings.putIfAbsent(oldBarcode, newBarcode);
    }

    /** 将 0-based 列索引转换为 Excel 列字母（A、B、…、Z、AA）。 */
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
