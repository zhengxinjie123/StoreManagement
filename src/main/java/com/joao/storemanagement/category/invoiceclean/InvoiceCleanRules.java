package com.joao.storemanagement.category.invoiceclean;

import cn.hutool.core.util.StrUtil;
import com.joao.storemanagement.entity.primary.InvoiceTemplate;
import com.joao.storemanagement.utils.ExcelCellReader;
import org.apache.poi.ss.usermodel.Sheet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.joao.storemanagement.category.invoiceclean.InvoiceCleanSupport.DEFAULT_TAX_RATE;
import static com.joao.storemanagement.category.invoiceclean.InvoiceCleanSupport.appendName;
import static com.joao.storemanagement.category.invoiceclean.InvoiceCleanSupport.calcTaxExcludedPrice;
import static com.joao.storemanagement.category.invoiceclean.InvoiceCleanSupport.calcTaxIncludedPrice;
import static com.joao.storemanagement.category.invoiceclean.InvoiceCleanSupport.calcLineTotalIncTax;
import static com.joao.storemanagement.category.invoiceclean.InvoiceCleanSupport.hasTaxRateColumn;
import static com.joao.storemanagement.category.invoiceclean.InvoiceCleanSupport.isValidBarcode;
import static com.joao.storemanagement.category.invoiceclean.InvoiceCleanSupport.isZeroPricePalletRow;
import static com.joao.storemanagement.category.invoiceclean.InvoiceCleanSupport.money;
import static com.joao.storemanagement.category.invoiceclean.InvoiceCleanSupport.readLineSubtotal;
import static com.joao.storemanagement.category.invoiceclean.InvoiceCleanSupport.readTaxRateFromColumn;
import static com.joao.storemanagement.category.invoiceclean.InvoiceCleanSupport.readUnitPriceExTax;
import static com.joao.storemanagement.category.invoiceclean.InvoiceCleanSupport.readUnitPriceIncTax;
import static com.joao.storemanagement.category.invoiceclean.InvoiceCleanSupport.resolveName;
import static com.joao.storemanagement.category.invoiceclean.InvoiceCleanSupport.splitMixedName;

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
        int filteredCount = 0;
        BigDecimal filteredAmount = BigDecimal.ZERO;
        // 原条码 -> 新条码，按发票行顺序保留，同一原条码只记录首次映射
        Map<String, String> barcodeMappings = new LinkedHashMap<>();
        // 模板 dataStartRow 为 1-based，POI 行索引为 0-based
        int startRow = template.getDataStartRow() - 1;
        int lastRow = sheet.getLastRowNum();

        for (int rowIndex = startRow; rowIndex <= lastRow; rowIndex++) {
            // 命中 Subtotal 合计行时终止扫描，避免页脚字段被误读为商品行
            if (shouldBreakAtRow(sheet, rowIndex, template, options)) {
                break;
            }

            // 读取条码列，作为有效商品行的首要判定依据
            String barcode = ExcelCellReader.readString(sheet, rowIndex, template.getBarcodeCol());

            // 解析中/外文品名（独立列或混合列拆分）
            InvoiceCleanSupport.NameParts nameParts = resolveNameParts(sheet, rowIndex, template, options);

            if (!isValidBarcode(barcode)) {
                if (options.isFilterRowsWithoutBarcode()) {
                    // 无条码但有数量的行视为无效商品，计入过滤统计
                    BigDecimal quantity = ExcelCellReader.readDecimal(sheet, rowIndex, template.getQuantityCol());
                    if (quantity == null) {
                        // 仅有品名续行时，合并到上一有效行的名称字段
                        appendName(rows, nameParts);
                        continue;
                    }
                    filteredCount++;
                    // 估算被过滤行的含税金额，供汇总备注使用
                    filteredAmount = filteredAmount.add(
                            estimateLineAmount(sheet, rowIndex, template, taxIncluded, quantity, options));
                    continue;
                }
                // 不过滤时，将无条码续行品名合并到上一行
                appendName(rows, nameParts);
                continue;
            }

            // 读取数量列，空数量视为非商品行
            BigDecimal quantity = ExcelCellReader.readDecimal(sheet, rowIndex, template.getQuantityCol());
            if (quantity == null) {
                appendName(rows, nameParts);
                continue;
            }

            // 中英文品名均为空时跳过
            if (StrUtil.isBlank(nameParts.chinese()) && StrUtil.isBlank(nameParts.foreignName())) {
                continue;
            }

            // 解析行税率：优先列值，否则模板默认或系统默认
            BigDecimal taxRate = resolveTaxRate(sheet, rowIndex, template, options);
            // 配置了税率列且要求仅读列值时，缺税率的行直接跳过
            if (options.isTaxRateFromColumnOnly() && hasTaxRateColumn(template) && taxRate == null) {
                continue;
            }

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
                if (lineSubtotalColumnIsTaxIncluded(template, taxIncluded, options)) {
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
            row.setBarcode(StrUtil.trim(barcode));
            row.setChineseName(nameParts.chinese());
            row.setForeignName(nameParts.foreignName());
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
                .barcodeMappingRemark(InvoiceCleanSupport.formatBarcodeMappingRemark(barcodeMappings))
                .build();
    }

    /**
     * 解析当前行的中/外文品名。
     * 模板配置了中文名列时走标准双列读取；否则按 options 决定是否从外文列拆分混合品名。
     */
    private static InvoiceCleanSupport.NameParts resolveNameParts(Sheet sheet, int rowIndex,
                                                                  InvoiceTemplate template,
                                                                  InvoiceCleanOptions options) {
        if (StrUtil.isNotBlank(template.getChineseNameCol())) {
            return resolveName(sheet, rowIndex, template);
        }
        String rawName = ExcelCellReader.readString(sheet, rowIndex, template.getForeignNameCol());
        if (options.isSplitMixedChineseForeignName()) {
            // 从同一列文本中拆分中文与外文片段
            return splitMixedName(rawName);
        }
        return new InvoiceCleanSupport.NameParts("", StrUtil.trim(rawName));
    }

    /**
     * 判断行小计列是否表示含税金额。
     * 当模板仅配置含税价列、未配置不含税价列且整体为含税模式时为 true。
     */
    static boolean lineSubtotalColumnIsTaxIncluded(InvoiceTemplate template, boolean taxIncluded, InvoiceCleanOptions options) {
        if (options.isIncludeTaxRateSubTotal()) {
            return true;
        }
        return taxIncluded
                && StrUtil.isBlank(template.getPriceCol())
                && StrUtil.isNotBlank(template.getPriceTaxIncludedCol());
    }

    /**
     * 解析行税率：优先读税率列；列值为空时按 options 决定是否回退模板默认税率。
     */
    private static BigDecimal resolveTaxRate(Sheet sheet, int rowIndex, InvoiceTemplate template,
                                             InvoiceCleanOptions options) {
        BigDecimal taxRate = readTaxRateFromColumn(sheet, rowIndex, template, options);
        if (taxRate != null) {
            return taxRate;
        }
        // 要求仅读列值且已配置税率列时，不回退默认税率
        if (options.isTaxRateFromColumnOnly() && hasTaxRateColumn(template)) {
            return null;
        }
        return template.getDefaultTaxRate() == null ? DEFAULT_TAX_RATE : template.getDefaultTaxRate();
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
            if (lineSubtotalColumnIsTaxIncluded(template, taxIncluded, options)) {
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
     * 判断当前行是否为 Subtotal 停扫点。
     * 依次检查条码列、整行扫描（breakOnSubtotalRow）、指定列（subtotalStopColumn）三种策略。
     */
    private static boolean shouldBreakAtRow(Sheet sheet, int rowIndex, InvoiceTemplate template,
                                           InvoiceCleanOptions options) {
        String barcode = ExcelCellReader.readString(sheet, rowIndex, template.getBarcodeCol());
        if (StrUtil.equalsIgnoreCase(StrUtil.trim(barcode), "Subtotal")) {
            return true;
        }
        if (options.isBreakOnSubtotalRow()) {
            // 小计标签可能出现在条码列以外的任意列（如飞跃发票）
            for (int col = 0; col <= 20; col++) {
                String text = ExcelCellReader.readString(sheet, rowIndex, columnLetter(col));
                if (StrUtil.equalsIgnoreCase(StrUtil.trim(text), "Subtotal")) {
                    return true;
                }
            }
        }
        if (StrUtil.isNotBlank(options.getSubtotalStopColumn())) {
            // 仅检查指定列的小计标签（飞跃发票在 C 列标记合计行）
            String label = ExcelCellReader.readString(sheet, rowIndex, options.getSubtotalStopColumn());
            return StrUtil.equalsIgnoreCase(StrUtil.trim(label), "Subtotal");
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
