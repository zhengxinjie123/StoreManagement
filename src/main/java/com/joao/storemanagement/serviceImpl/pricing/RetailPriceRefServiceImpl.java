package com.joao.storemanagement.serviceImpl.pricing;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joao.storemanagement.dto.pricing.RetailPriceColumnMappingDTO;
import com.joao.storemanagement.dto.pricing.RetailPriceExcelPreviewDTO;
import com.joao.storemanagement.dto.pricing.RetailPriceMappedLinePreviewDTO;
import com.joao.storemanagement.dto.pricing.RetailPriceSourceRequestDTO;
import com.joao.storemanagement.entity.pricing.RetailPriceRefLine;
import com.joao.storemanagement.entity.pricing.RetailPriceSource;
import com.joao.storemanagement.mapper.primary.pricing.RetailPriceRefLineMapper;
import com.joao.storemanagement.mapper.primary.pricing.RetailPriceSourceMapper;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.service.pricing.RetailPriceRefService;
import com.joao.storemanagement.utils.BarcodeUtil;
import com.joao.storemanagement.utils.pricing.RetailPriceRefExcelParser;
import com.joao.storemanagement.utils.pricing.RetailPriceRefExcelParser.ParsedRefLine;
import com.joao.storemanagement.vo.pricing.RetailPriceRefLineVO;
import com.joao.storemanagement.vo.response.PageResponseVO;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RetailPriceRefServiceImpl implements RetailPriceRefService {

    private static final Logger log = LoggerFactory.getLogger(RetailPriceRefServiceImpl.class);
    private static final int PARSE_PREVIEW_LIMIT = 50;
    private static final int MAX_PRODUCT_NAME_LEN = 2000;

    private final RetailPriceSourceMapper sourceMapper;
    private final RetailPriceRefLineMapper lineMapper;
    private final RetailPriceRefExcelParser excelParser;
    private final ObjectMapper objectMapper;
    private final RetailPriceRefLineBatchInserter lineBatchInserter;

    public RetailPriceRefServiceImpl(
            RetailPriceSourceMapper sourceMapper,
            RetailPriceRefLineMapper lineMapper,
            RetailPriceRefExcelParser excelParser,
            ObjectMapper objectMapper,
            RetailPriceRefLineBatchInserter lineBatchInserter) {
        this.sourceMapper = sourceMapper;
        this.lineMapper = lineMapper;
        this.excelParser = excelParser;
        this.objectMapper = objectMapper;
        this.lineBatchInserter = lineBatchInserter;
    }

    @Override
    public List<RetailPriceSource> listSources() {
        return sourceMapper.selectAll();
    }

    @Override
    public void requireSourceExists(Long sourceId) {
        if (sourceMapper.selectById(sourceId) == null) {
            throw new BusinessException("参考源不存在");
        }
    }

    @Override
    @Transactional
    public RetailPriceSource createSource(RetailPriceSourceRequestDTO request) {
        if (sourceMapper.selectByCode(request.code()) != null) {
            throw new BusinessException("参考源编码已存在: " + request.code());
        }
        RetailPriceSource row = toEntity(request);
        sourceMapper.insert(row);
        return sourceMapper.selectById(row.getId());
    }

    @Override
    @Transactional
    public RetailPriceSource updateSource(Long id, RetailPriceSourceRequestDTO request) {
        RetailPriceSource existing = sourceMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("参考源不存在");
        }
        RetailPriceSource other = sourceMapper.selectByCode(request.code());
        if (other != null && !other.getId().equals(id)) {
            throw new BusinessException("参考源编码已存在: " + request.code());
        }
        RetailPriceSource row = toEntity(request);
        row.setId(id);
        sourceMapper.update(row);
        return sourceMapper.selectById(id);
    }

    @Override
    @Transactional
    public int clearSourceLines(Long sourceId) {
        if (sourceMapper.selectById(sourceId) == null) {
            throw new BusinessException("参考源不存在");
        }
        return lineMapper.deleteBySourceId(sourceId);
    }

    @Override
    public RetailPriceExcelPreviewDTO previewImportExcel(MultipartFile file) {
        return excelParser.preview(file);
    }

    @Override
    public List<RetailPriceMappedLinePreviewDTO> previewMappedLines(
            MultipartFile file, String mappingJson, String barcodeFilter) {
        RetailPriceColumnMappingDTO mapping = parseMapping(mappingJson);
        return excelParser.parsePreview(file, mapping, PARSE_PREVIEW_LIMIT, barcodeFilter);
    }

    @Override
    @Transactional
    public Map<String, Object> importExcel(Long sourceId, MultipartFile file, String mappingJson) {
        RetailPriceSource source = sourceMapper.selectById(sourceId);
        if (source == null) {
            throw new BusinessException("参考源不存在");
        }
        try {
            RetailPriceColumnMappingDTO mapping = parseMapping(mappingJson);
            List<ParsedRefLine> parsed = excelParser.parse(file, mapping);
            lineMapper.deleteBySourceId(sourceId);

            Map<String, RetailPriceRefLine> deduped = new LinkedHashMap<>();
            int duplicateBarcodes = 0;
            for (ParsedRefLine line : parsed) {
                String barcodeKey = BarcodeUtil.judgeBarcode(line.barcode());
                RetailPriceRefLine row = toRefLine(sourceId, line);
                if (deduped.containsKey(barcodeKey)) {
                    duplicateBarcodes++;
                    RetailPriceRefLine kept = deduped.get(barcodeKey);
                    if (pricePrecisionScore(row) <= pricePrecisionScore(kept)) {
                        continue;
                    }
                }
                deduped.put(barcodeKey, row);
            }

            List<RetailPriceRefLine> rows = new ArrayList<>(deduped.values());
            insertLines(rows);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("sourceId", sourceId);
            result.put("sourceCode", source.getCode());
            result.put("imported", rows.size());
            result.put("parsed", parsed.size());
            result.put("duplicateBarcodes", duplicateBarcodes);
            result.put("mapping", mapping);
            return result;
        } catch (BusinessException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            log.error("import excel failed sourceId={} file={}", sourceId, file.getOriginalFilename(), ex);
            throw new BusinessException("导入失败: " + rootCauseMessage(ex), ex);
        }
    }

    @Override
    public int countLines(Long sourceId) {
        return lineMapper.countBySourceId(sourceId);
    }

    @Override
    public PageResponseVO<RetailPriceRefLineVO> pageLines(
            Long sourceId, long current, long pageSize, String barcode, String productName) {
        requireSourceExists(sourceId);
        long safeCurrent = Math.max(current, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 200);
        String normalizedBarcode = StrUtil.trimToNull(barcode);
        String normalizedProductName = StrUtil.trimToNull(productName);
        long offset = (safeCurrent - 1) * safePageSize;
        long total = lineMapper.countPage(sourceId, normalizedBarcode, normalizedProductName);
        List<RetailPriceRefLineVO> records = lineMapper
                .selectPage(sourceId, normalizedBarcode, normalizedProductName, offset, safePageSize)
                .stream()
                .map(RetailPriceRefLineVO::of)
                .toList();
        return PageResponseVO.of(safeCurrent, safePageSize, total, records);
    }

    @Override
    @Transactional
    public void deleteLine(Long sourceId, Long lineId) {
        requireSourceExists(sourceId);
        RetailPriceRefLine line = lineMapper.selectById(lineId);
        if (line == null || !sourceId.equals(line.getSourceId())) {
            throw new BusinessException("参考源明细不存在");
        }
        if (lineMapper.deleteById(lineId) == 0) {
            throw new BusinessException("参考源明细不存在");
        }
    }

    @Override
    public String exportLinesCsv(Long sourceId, String barcode, String productName) {
        requireSourceExists(sourceId);
        String normalizedBarcode = StrUtil.trimToNull(barcode);
        String normalizedProductName = StrUtil.trimToNull(productName);
        List<RetailPriceRefLine> lines =
                lineMapper.selectForExport(sourceId, normalizedBarcode, normalizedProductName);
        StringBuilder builder = new StringBuilder("条码,商品名,含税进价,含税售价\n");
        for (RetailPriceRefLine line : lines) {
            builder.append(csv(line.getBarcode())).append(',')
                    .append(csv(line.getProductName())).append(',')
                    .append(line.getPurchasePriceTax()).append(',')
                    .append(line.getRetailPrice()).append('\n');
        }
        return builder.toString();
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private RetailPriceColumnMappingDTO parseMapping(String mappingJson) {
        if (StrUtil.isBlank(mappingJson)) {
            throw new BusinessException("请指定 Excel 列映射");
        }
        try {
            return objectMapper.readValue(mappingJson, RetailPriceColumnMappingDTO.class);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("列映射格式无效", ex);
        }
    }

    /** 使用批量插入提交参考价明细，避免多条 INSERT 的 BigDecimal 绑定问题。 */
    private void insertLines(List<RetailPriceRefLine> rows) {
        for (RetailPriceRefLine row : rows) {
            scalePricesForDb(row);
        }
        lineBatchInserter.insertAll(rows);
    }

    private static RetailPriceRefLine toRefLine(Long sourceId, ParsedRefLine line) {
        RetailPriceRefLine row = new RetailPriceRefLine();
        row.setSourceId(sourceId);
        row.setBarcode(line.barcode());
        row.setProductName(truncateProductName(line.productName()));
        row.setPurchasePriceTax(line.purchasePriceTax());
        row.setRetailPrice(line.retailPriceTax());
        return row;
    }

    private static void scalePricesForDb(RetailPriceRefLine row) {
        if (row.getPurchasePriceTax() != null) {
            row.setPurchasePriceTax(row.getPurchasePriceTax().setScale(4, RoundingMode.HALF_UP));
        }
        if (row.getRetailPrice() != null) {
            row.setRetailPrice(row.getRetailPrice().setScale(4, RoundingMode.HALF_UP));
        }
    }

    /**
     * 重复条码时优先保留小数位更多的价格，避免 Excel 整数显示格式覆盖真实价格。
     */
    private static int pricePrecisionScore(RetailPriceRefLine row) {
        return decimalPrecision(row.getRetailPrice()) * 10 + decimalPrecision(row.getPurchasePriceTax());
    }

    private static int decimalPrecision(java.math.BigDecimal value) {
        if (value == null) {
            return 0;
        }
        return Math.max(0, value.stripTrailingZeros().scale());
    }

    private static String truncateProductName(String name) {
        if (name == null) {
            return null;
        }
        return name.length() <= MAX_PRODUCT_NAME_LEN ? name : name.substring(0, MAX_PRODUCT_NAME_LEN);
    }

    private static String rootCauseMessage(Throwable ex) {
        Throwable cur = ex;
        String message = ex.getMessage();
        while (cur.getCause() != null) {
            cur = cur.getCause();
            if (cur.getMessage() != null && !cur.getMessage().isBlank()) {
                message = cur.getMessage();
            }
        }
        return message != null ? message : ex.getClass().getSimpleName();
    }

    private RetailPriceSource toEntity(RetailPriceSourceRequestDTO request) {
        RetailPriceSource row = new RetailPriceSource();
        row.setCode(StrUtil.trim(request.code()));
        row.setName(StrUtil.trim(request.name()));
        row.setPriority(request.priority());
        row.setRemark(request.remark());
        row.setActive(request.active() == null || request.active());
        return row;
    }
}
