package com.joao.storemanagement.talent.purchase.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.joao.storemanagement.config.StoreProperties;
import com.joao.storemanagement.talent.purchase.dto.ProductImportDTO;
import com.joao.storemanagement.talent.purchase.dto.PurchaseImportResult;
import com.joao.storemanagement.talent.purchase.entity.Inventory;
import com.joao.storemanagement.talent.purchase.entity.Product;
import com.joao.storemanagement.talent.purchase.entity.Purchase;
import com.joao.storemanagement.talent.purchase.entity.PurchaseDetail;
import com.joao.storemanagement.talent.purchase.exception.ImportRowException;
import com.joao.storemanagement.talent.purchase.mapper.PurchaseDetailMapper;
import com.joao.storemanagement.talent.purchase.mapper.PurchaseHeaderMapper;
import com.joao.storemanagement.talent.purchase.mapper.PurchaseInventoryMapper;
import com.joao.storemanagement.talent.purchase.mapper.PurchaseParameterMapper;
import com.joao.storemanagement.talent.purchase.mapper.PurchaseProductMapper;
import com.joao.storemanagement.utils.BarcodeNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class PurchaseImportEngine {

    private static final String PARAM_LAST_AUTO_CODE_PURCHASE = "LastAutoCode_Purchase";
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final PurchaseProductMapper productMapper;
    private final PurchaseInventoryMapper inventoryMapper;
    private final PurchaseHeaderMapper purchaseHeaderMapper;
    private final PurchaseDetailMapper purchaseDetailMapper;
    private final PurchaseParameterMapper parameterMapper;
    private final PurchaseProductCodeService productCodeService;
    private final PurchaseSupplierService supplierService;
    private final PurchaseTaxRateService taxRateService;
    private final StoreProperties storeProperties;

    public PurchaseImportEngine(
            PurchaseProductMapper productMapper,
            PurchaseInventoryMapper inventoryMapper,
            PurchaseHeaderMapper purchaseHeaderMapper,
            PurchaseDetailMapper purchaseDetailMapper,
            PurchaseParameterMapper parameterMapper,
            PurchaseProductCodeService productCodeService,
            PurchaseSupplierService supplierService,
            PurchaseTaxRateService taxRateService,
            StoreProperties storeProperties) {
        this.productMapper = productMapper;
        this.inventoryMapper = inventoryMapper;
        this.purchaseHeaderMapper = purchaseHeaderMapper;
        this.purchaseDetailMapper = purchaseDetailMapper;
        this.parameterMapper = parameterMapper;
        this.productCodeService = productCodeService;
        this.supplierService = supplierService;
        this.taxRateService = taxRateService;
        this.storeProperties = storeProperties;
    }

    public PurchaseImportResult importFromExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传 Excel 文件");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !(fileName.endsWith(".xlsx") || fileName.endsWith(".xls"))) {
            throw new IllegalArgumentException("仅支持 .xlsx 或 .xls 格式");
        }
        ParsedImportSheet parsed = parseExcel(file);
        if (parsed.rows().isEmpty()) {
            throw new IllegalArgumentException("Excel 中没有有效商品行");
        }
        return importFromLines(fileName, null, null, parsed.rows(), parsed.taxIncluded());
    }

    public PurchaseImportResult importFromArchive(
            Path file, String supplierGuid, String invoiceNo, Boolean templateTaxIncluded) {
        if (file == null || !Files.exists(file)) {
            throw new IllegalArgumentException("归档发票文件不存在");
        }
        ParsedImportSheet parsed = parseExcel(file);
        if (parsed.rows().isEmpty()) {
            throw new IllegalArgumentException("归档发票中没有有效商品行");
        }
        boolean taxIncluded = resolveTaxIncluded(parsed.rows(), templateTaxIncluded, parsed.taxIncluded());
        return importFromLinesWithSupplierGuid(supplierGuid, invoiceNo, parsed.rows(), taxIncluded);
    }


    public PurchaseImportResult importFromLines(
            String fileName,
            String preferredSupplierName,
            String invoiceNo,
            List<ProductImportDTO> rows,
            Boolean taxIncluded) {
        String supplierGuid = supplierService.resolveSupplierGuid(fileName, preferredSupplierName, LocalDateTime.now());
        return importFromLinesWithSupplierGuid(supplierGuid, invoiceNo, rows, resolveTaxIncluded(rows, taxIncluded, null));
    }

    public PurchaseImportResult importFromLinesWithSupplierGuid(
            String supplierGuid,
            String invoiceNo,
            List<ProductImportDTO> rows,
            boolean taxIncluded) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("导入行不能为空");
        }
        if (StrUtil.isBlank(supplierGuid)) {
            throw new IllegalArgumentException("supplierGuid 不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        PurchaseSequence purchaseSequence = nextPurchaseSequence();

        Purchase purchase = buildPurchaseForImport(purchaseSequence, now, supplierGuid, invoiceNo, taxIncluded);
        purchaseHeaderMapper.insert(purchase);

        int lineNo = 0;
        int newProductCount = 0;
        int existingProductCount = 0;
        for (ProductImportDTO row : rows) {
            lineNo++;
            finalizeRowTax(row, lineNo);
            if (processRow(purchase, row, lineNo, now, supplierGuid)) {
                newProductCount++;
            } else {
                existingProductCount++;
            }
        }

        parameterMapper.updateStringValue(PARAM_LAST_AUTO_CODE_PURCHASE, purchaseSequence.sequencePart());
        log.info(
                "purchase import done purchaseNo={} lines={} newProducts={} existingProducts={}",
                purchase.getNo(),
                rows.size(),
                newProductCount,
                existingProductCount);
        return new PurchaseImportResult(
                purchase.getNo(),
                purchase.getGuid(),
                rows.size(),
                newProductCount,
                existingProductCount);
    }

    private boolean processRow(
            Purchase purchase, ProductImportDTO row, int lineNo, LocalDateTime now, String supplierGuid) {
        try {
            return processRowInternal(purchase, row, lineNo, now, supplierGuid);
        } catch (ImportRowException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ImportRowException(
                    lineNo,
                    row.getBarcode(),
                    StrUtil.blankToDefault(ex.getMessage(), "处理该行时发生未知错误"));
        }
    }

    private boolean processRowInternal(
            Purchase purchase, ProductImportDTO row, int lineNo, LocalDateTime now, String supplierGuid) {
        String barcode = BarcodeNormalizer.normalize(row.getBarcode());
        row.setBarcode(barcode);

        Product product = findExistingProduct(barcode);
        boolean isNew = product == null;
        StoreProperties.ProductDefaults productDefaults = storeProperties.getProduct();

        if (isNew) {
            String productNo = productCodeService.nextProductNo(productDefaults.getTypeGuid());
            product = row.toNewProduct(
                    UUID.randomUUID().toString(),
                    productNo,
                    productDefaults,
                    supplierGuid,
                    now);
            productMapper.insert(product);
        }

        PricePair prices = resolvePrices(row);
        Inventory inventory = resolveInventory(product, prices, now, supplierGuid);
        BigDecimal qty = BigDecimal.valueOf(row.getQuantity());

        PurchaseDetail detail = buildPurchaseDetail(
                purchase, product, inventory, row, lineNo, prices, qty, now);
        purchaseDetailMapper.insert(detail);

        applyReceive(product.getGuid(), inventory.getGuid(), prices, qty, now);
        return isNew;
    }

    private void applyReceive(
            String productGuid, String inventoryGuid, PricePair prices, BigDecimal qty, LocalDateTime now) {
        Product product = productMapper.selectById(productGuid);
        if (product == null) {
            throw new IllegalStateException("product not found: " + productGuid);
        }
        BigDecimal currentQty = nonNegativeBase(product.getQuantityBk());
        BigDecimal currentInvoiceQty = nonNegativeBase(product.getInvoiceQuantityBk());

        productMapper.update(
                Wrappers.lambdaUpdate(Product.class)
                        .eq(Product::getGuid, productGuid)
                        .set(Product::getQuantityBk, currentQty.add(qty))
                        .set(Product::getInvoiceQuantityBk, currentInvoiceQty.add(qty))
                        .set(Product::getPurchasePrice, prices.exTax())
                        .set(Product::getPurchasePriceTax, prices.inTax())
                        .set(Product::getUpdateDate, now)
                        .set(Product::getUpdateTime, now));

        Inventory inventory = inventoryMapper.selectById(inventoryGuid);
        if (inventory == null) {
            throw new IllegalStateException("inventory not found: " + inventoryGuid);
        }
        BigDecimal invQty = nonNegativeBase(inventory.getQuantity());
        BigDecimal invInvoiceQty = nonNegativeBase(inventory.getInvoiceQuantity());
        inventoryMapper.update(
                Wrappers.lambdaUpdate(Inventory.class)
                        .eq(Inventory::getGuid, inventoryGuid)
                        .set(Inventory::getQuantity, invQty.add(qty))
                        .set(Inventory::getInvoiceQuantity, invInvoiceQty.add(qty))
                        .set(Inventory::getUnitPrice, prices.exTax())
                        .set(Inventory::getUnitPriceTax, prices.inTax())
                        .set(Inventory::getPurchaseDate, now));
    }

    private Inventory resolveInventory(
            Product product, PricePair prices, LocalDateTime now, String supplierGuid) {
        String depotGuid = storeProperties.getProduct().getDepotGuid();
        String batchNo = storeProperties.getProduct().getBatchNo();
        List<Inventory> existing = inventoryMapper.selectList(
                Wrappers.lambdaQuery(Inventory.class)
                        .eq(Inventory::getProductGuid, product.getGuid())
                        .eq(Inventory::getDepotGuid, depotGuid)
                        .eq(Inventory::getBatchNo, batchNo));

        if (existing.size() == 1) {
            return existing.get(0);
        }
        if (existing.size() > 1) {
            throw new IllegalStateException("multiple inventory rows for product " + product.getNo());
        }

        Inventory inventory = new Inventory();
        inventory.setGuid(UUID.randomUUID().toString());
        inventory.setDepotGuid(depotGuid);
        inventory.setProductGuid(product.getGuid());
        inventory.setSupplierGuid(supplierGuid);
        inventory.setBatchNo(batchNo);
        inventory.setUnitPrice(prices.exTax());
        inventory.setUnitPriceTax(prices.inTax());
        inventory.setPurchaseDate(now);
        inventory.setUsefulLife(now);
        inventory.setQuantity(ZERO);
        inventory.setRemark("");
        inventory.setInvoiceQuantity(ZERO);
        inventory.setDiffQuantity(ZERO);
        inventoryMapper.insert(inventory);
        return inventory;
    }

    private PurchaseDetail buildPurchaseDetail(
            Purchase purchase,
            Product product,
            Inventory inventory,
            ProductImportDTO row,
            int lineNo,
            PricePair prices,
            BigDecimal qty,
            LocalDateTime now) {
        StoreProperties.PurchaseDefaults purchaseDefaults = storeProperties.getPurchase();
        PurchaseDetail detail = new PurchaseDetail();
        detail.setGuid(UUID.randomUUID().toString());
        detail.setMasterGuid(purchase.getGuid());
        detail.setNo(String.format("%04d", lineNo));
        detail.setInventoryGuid(inventory.getGuid());
        detail.setProductGuid(product.getGuid());
        detail.setDepotGuid(storeProperties.getProduct().getDepotGuid());
        detail.setBatchNo(storeProperties.getProduct().getBatchNo());
        detail.setUsefulLife(now);
        detail.setQuantity(qty);
        detail.setUnitPrice(prices.exTax());
        detail.setTaxRate(row.effectiveTaxRate());
        detail.setUnitPriceTax(prices.inTax());
        detail.setDiscountRate(ONE);
        detail.setUnitPriceFact(prices.exTax());
        detail.setAmount(prices.exTax().multiply(qty).setScale(4, RoundingMode.HALF_UP));
        detail.setInvoiceQuantity(qty);
        detail.setProductNo(product.getNo());
        detail.setName(
                StrUtil.isNotBlank(row.getNameCn())
                        ? row.getNameCn()
                        : StrUtil.nullToEmpty(product.getName()));
        detail.setNameP(
                StrUtil.isNotBlank(row.getName())
                        ? row.getName()
                        : StrUtil.nullToEmpty(product.getNameP()));
        detail.setProductUnitName(purchaseDefaults.getProductUnitName());
        detail.setBarcode(StrUtil.nullToEmpty(row.getBarcode()));
        detail.setCombinable(!Boolean.FALSE.equals(product.getCombinable()) && product.getCombinable());
        detail.setPackageQuantity(product.getPackageQuantity() != null ? product.getPackageQuantity() : ONE);
        detail.setPzCost(ZERO);
        detail.setCheckQuantity(qty);
        detail.setPurchasePriceBk(prices.exTax());
        detail.setRetailPriceTaxBk(ZERO);
        detail.setWholesalePriceBk(ZERO);
        return detail;
    }

    private Purchase buildPurchaseForImport(
            PurchaseSequence sequence,
            LocalDateTime now,
            String supplierGuid,
            String invoiceNo,
            boolean taxIncluded) {
        return buildPurchase(sequence, now, supplierGuid, invoiceNo, taxIncluded);
    }

    private Purchase buildPurchase(
            PurchaseSequence sequence, LocalDateTime now, String supplierGuid, String invoiceNo, boolean taxIncluded) {
        StoreProperties.PurchaseDefaults defaults = storeProperties.getPurchase();
        Purchase purchase = new Purchase();
        purchase.setGuid(UUID.randomUUID().toString());
        purchase.setSupplierGuid(supplierGuid);
        purchase.setDateTime(now);
        purchase.setNo(sequence.purchaseNo());
        purchase.setContact(null);
        purchase.setTel(null);
        purchase.setFax(null);
        purchase.setPremium(ZERO);
        purchase.setPayment(ZERO);
        purchase.setPaymentDate(now);
        if (StrUtil.isNotBlank(invoiceNo)) {
            purchase.setRemark("商店管理系统导入 " + invoiceNo);
        } else {
            purchase.setRemark("商店管理系统导入");
        }
        purchase.setEmployeeGuid(defaults.getEmployeeGuid());
        purchase.setMarkerUserGuid(defaults.getMarkerUserGuid());
        purchase.setApproverUserGuid(defaults.getApproverUserGuid());
        purchase.setApproved(true);
        purchase.setCanBeAntiApprove(true);
        purchase.setClosed(false);
        purchase.setCanBeAntiClose(true);
        purchase.setSimPrnIva(false);
        purchase.setSimPrnCh(true);
        purchase.setInvoice(true);
        purchase.setCIva(taxIncluded);
        return purchase;
    }

    private static boolean resolveTaxIncluded(
            List<ProductImportDTO> rows, Boolean templateTaxIncluded, Boolean parsedFromHeader) {
        if (templateTaxIncluded != null) {
            return templateTaxIncluded;
        }
        if (parsedFromHeader != null) {
            return parsedFromHeader;
        }
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        return Boolean.TRUE.equals(rows.get(0).getPriceIncludesTax());
    }

    private PurchaseSequence nextPurchaseSequence() {
        String last = StrUtil.trimToEmpty(parameterMapper.selectStringValue(PARAM_LAST_AUTO_CODE_PURCHASE));
        int next = 1;
        if (!last.isEmpty()) {
            try {
                next = Integer.parseInt(last) + 1;
            } catch (NumberFormatException ex) {
                log.warn("invalid LastAutoCode_Purchase={}, reset to 1", last);
            }
        }
        String sequencePart = String.format("%04d", next);
        String purchaseNo = LocalDate.now().getYear() + sequencePart;
        return new PurchaseSequence(purchaseNo, sequencePart);
    }

    private Product findExistingProduct(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        Product byBarcode = productMapper.selectByBarcode(key);
        if (byBarcode != null) {
            return byBarcode;
        }
        return productMapper.selectByProductNo(key);
    }

    private PricePair resolvePrices(ProductImportDTO row) {
        BigDecimal taxRate = row.effectiveTaxRate();
        BigDecimal input = row.getPrice();
        if (Boolean.TRUE.equals(row.getPriceIncludesTax())) {
            BigDecimal exTax = input.divide(ONE.add(taxRate), 4, RoundingMode.HALF_UP);
            return new PricePair(exTax, input);
        }
        return new PricePair(input, input.multiply(ONE.add(taxRate)).setScale(4, RoundingMode.HALF_UP));
    }

    private ParsedImportSheet parseExcel(MultipartFile file) {
        try {
            return parseExcel(file.getInputStream());
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("解析 Excel 失败：" + ex.getMessage(), ex);
        }
    }

    private ParsedImportSheet parseExcel(Path file) {
        try (InputStream inputStream = Files.newInputStream(file)) {
            return parseExcel(inputStream);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("解析归档发票失败：" + ex.getMessage(), ex);
        }
    }

    private ParsedImportSheet parseExcel(InputStream inputStream) {
        List<ProductImportDTO> importRows = new ArrayList<>();
        boolean taxIncluded;
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Excel 第一行必须是表头");
            }

            String dHeader = getCellValue(headerRow.getCell(3));
            if ("含税价".equals(dHeader) || "含税单价".equals(dHeader) || "进价含税".equals(dHeader)) {
                taxIncluded = true;
            } else if ("不含税价".equals(dHeader) || "不含税单价".equals(dHeader) || "进价不含税".equals(dHeader)) {
                taxIncluded = false;
            } else {
                throw new IllegalArgumentException(
                        "表头 D 列必须是「含税价/含税单价/进价含税」或「不含税价/不含税单价/进价不含税」，当前为：" + dHeader);
            }

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String barcode = BarcodeNormalizer.normalize(getCellValue(row.getCell(0)));
                String name = getCellValue(row.getCell(1));
                String qtyStr = getCellValue(row.getCell(2));
                String priceStr = getCellValue(row.getCell(3));
                String taxStr = getCellValue(row.getCell(4));

                if (StrUtil.isBlank(barcode) && StrUtil.isBlank(name)) {
                    continue;
                }

                ProductImportDTO dto = new ProductImportDTO();
                dto.setBarcode(barcode);
                dto.setName(name);
                dto.setPriceIncludesTax(taxIncluded);
                if (StrUtil.isNotBlank(qtyStr)) {
                    dto.setQuantity(new BigDecimal(qtyStr).intValue());
                }
                if (StrUtil.isNotBlank(priceStr)) {
                    dto.setPrice(new BigDecimal(priceStr));
                }
                dto.setTaxRate(parseTaxRate(taxStr, rowIndex + 1, barcode));
                validateImportRow(dto, rowIndex + 1);
                finalizeRowTax(dto, rowIndex + 1);
                importRows.add(dto);
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("解析 Excel 失败：" + ex.getMessage(), ex);
        }
        return new ParsedImportSheet(importRows, taxIncluded);
    }

    private void finalizeRowTax(ProductImportDTO dto, int rowNumber) {
        BigDecimal raw = dto.getTaxRate() != null ? dto.getTaxRate() : taxRateService.defaultRate();
        dto.setTaxRate(taxRateService.resolveAllowedRate(raw, rowNumber, dto.getBarcode()));
    }

    private static void validateImportRow(ProductImportDTO dto, int rowNumber) {
        if (StrUtil.isBlank(dto.getBarcode())) {
            throw new ImportRowException(rowNumber, dto.getBarcode(), "列 A 条码不能为空");
        }
        if (StrUtil.isBlank(dto.getName())) {
            throw new ImportRowException(rowNumber, dto.getBarcode(), "列 B 品名不能为空");
        }
        if (dto.getQuantity() == null) {
            throw new ImportRowException(rowNumber, dto.getBarcode(), "列 C 数量不能为空");
        }
        if (dto.getQuantity() <= 0) {
            throw new ImportRowException(rowNumber, dto.getBarcode(), "数量必须大于 0");
        }
        if (dto.getPrice() == null) {
            throw new ImportRowException(rowNumber, dto.getBarcode(), "列 D 价格不能为空");
        }
        if (dto.getPrice().signum() < 0) {
            throw new ImportRowException(rowNumber, dto.getBarcode(), "价格不能为负数");
        }
    }

    private static BigDecimal parseTaxRate(String raw, int rowNumber, String barcode) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String s = raw.trim().replace("%", "").replace("％", "").trim();
        if (s.isEmpty()) {
            return null;
        }
        BigDecimal value;
        try {
            value = new BigDecimal(s);
        } catch (NumberFormatException ex) {
            throw new ImportRowException(rowNumber, barcode, "列 E 税率格式不正确：" + raw);
        }
        if (value.signum() < 0) {
            throw new ImportRowException(rowNumber, barcode, "税率不能为负数");
        }
        if (value.compareTo(ONE) > 0) {
            value = value.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        }
        return value;
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        return new DataFormatter().formatCellValue(cell).trim();
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : ZERO;
    }

    private static BigDecimal nonNegativeBase(BigDecimal value) {
        BigDecimal base = nullToZero(value);
        return base.signum() < 0 ? ZERO : base;
    }

    private record ParsedImportSheet(List<ProductImportDTO> rows, boolean taxIncluded) {}

    private record PurchaseSequence(String purchaseNo, String sequencePart) {}

    private record PricePair(BigDecimal exTax, BigDecimal inTax) {}
}
