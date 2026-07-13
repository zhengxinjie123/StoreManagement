package com.joao.storemanagement.serviceImpl.replenish;

import cn.hutool.core.util.StrUtil;
import com.joao.storemanagement.dto.replenish.PurchaseSuggestionGroupDTO;
import com.joao.storemanagement.dto.replenish.PurchaseSuggestionItemDTO;
import com.joao.storemanagement.dto.replenish.ReplenishRecordItemDTO;
import com.joao.storemanagement.dto.replenish.ReplenishRecordRequestDTO;
import com.joao.storemanagement.dto.replenish.ReplenishRecordTreeNodeDTO;
import com.joao.storemanagement.dto.talent.LatestPurchaseDTO;
import com.joao.storemanagement.dto.talent.ProductDTO;
import com.joao.storemanagement.entity.replenish.ReplenishRecord;
import com.joao.storemanagement.mapper.primary.replenish.ReplenishRecordMapper;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.mapper.talent.PosInventoryMapper;
import com.joao.storemanagement.mapper.talent.PosProductMapper;
import com.joao.storemanagement.mapper.talent.PosPurchaseMapper;
import com.joao.storemanagement.service.replenish.ReplenishRecordService;
import com.joao.storemanagement.service.talent.TalentOposDataSourceService;
import com.joao.storemanagement.vo.response.PageResponseVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReplenishRecordServiceImpl implements ReplenishRecordService {

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_DONE = "done";
    private static final String UNKNOWN_SUPPLIER = "未知供应商";
    private static final DateTimeFormatter EXPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReplenishRecordMapper mapper;
    private final TalentOposDataSourceService talentOposDataSourceService;

    public ReplenishRecordServiceImpl(
            ReplenishRecordMapper mapper,
            TalentOposDataSourceService talentOposDataSourceService) {
        this.mapper = mapper;
        this.talentOposDataSourceService = talentOposDataSourceService;
    }

    @Override
    @Transactional
    public List<ReplenishRecordTreeNodeDTO> tree() {
        syncAutoCompleteFromPurchases();
        return buildTree(mapper.selectPendingOrdered());
    }

    @Override
    public List<ReplenishRecordTreeNodeDTO> completedTree() {
        return buildTree(mapper.selectCompletedOrdered());
    }

    @Override
    public PageResponseVO<ReplenishRecord> page(
            long current,
            long pageSize,
            String status,
            String barcode,
            String supplierName,
            String keyword) {
        long safeCurrent = Math.max(current, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 200);
        String normalizedStatus = normalizeStatus(status);
        String normalizedBarcode = StrUtil.trimToNull(barcode);
        String normalizedSupplierName = StrUtil.trimToNull(supplierName);
        String normalizedKeyword = StrUtil.trimToNull(keyword);
        long offset = (safeCurrent - 1) * safePageSize;
        long total = mapper.countPage(
                normalizedStatus, normalizedBarcode, normalizedSupplierName, normalizedKeyword);
        List<ReplenishRecord> records = mapper.selectPage(
                normalizedStatus,
                normalizedBarcode,
                normalizedSupplierName,
                normalizedKeyword,
                offset,
                safePageSize);
        return PageResponseVO.of(safeCurrent, safePageSize, total, records);
    }

    @Override
    @Transactional
    public ReplenishRecord create(ReplenishRecordRequestDTO request) {
        String barcode = StrUtil.trim(request.barcode());
        if (mapper.selectPendingByBarcode(barcode) != null) {
            throw new BusinessException("该商品已在待补货列表，无需重复登记");
        }
        ProductDTO product = talentOposDataSourceService.executeInSession(
                session -> session.getMapper(PosProductMapper.class).selectByBarcode(barcode));
        if (product == null) {
            throw new BusinessException("未找到条码完全一致的商品: " + barcode);
        }
        BigDecimal inventory = talentOposDataSourceService.executeInSession(session -> session
                .getMapper(PosInventoryMapper.class)
                .sumQuantityByProductGuid(product.guid()));

        ReplenishRecord row = new ReplenishRecord();
        row.setBarcode(barcode);
        row.setNameChinese(product.nameChinese());
        row.setNameForeign(product.nameForeign());
        row.setSupplierName(StrUtil.blankToDefault(product.supplierName(), UNKNOWN_SUPPLIER));
        row.setInventoryQty(inventory);
        row.setRemark(request.remark());
        row.setStatus(STATUS_PENDING);
        mapper.insert(row);
        return mapper.selectById(row.getId());
    }

    @Override
    @Transactional
    public void complete(Long id) {
        if (mapper.updateStatusIfCurrent(id, STATUS_PENDING, STATUS_DONE) == 0) {
            throw new BusinessException("补货记录不存在或已完成");
        }
    }

    @Override
    @Transactional
    public void reopen(Long id) {
        ReplenishRecord row = mapper.selectById(id);
        if (row == null || !STATUS_DONE.equals(row.getStatus())) {
            throw new BusinessException("补货记录不存在或未完成");
        }
        if (mapper.selectPendingByBarcode(row.getBarcode()) != null) {
            throw new BusinessException("该商品已在待补货列表，无法恢复");
        }
        if (mapper.updateStatusIfCurrent(id, STATUS_DONE, STATUS_PENDING) == 0) {
            throw new BusinessException("补货记录不存在或未完成");
        }
    }

    @Override
    @Transactional
    public void updateRemark(Long id, String remark) {
        if (mapper.selectById(id) == null) {
            throw new BusinessException("补货记录不存在");
        }
        mapper.updateRemark(id, remark);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (mapper.deleteById(id) == 0) {
            throw new BusinessException("replenish record not found");
        }
    }

    @Override
    public List<PurchaseSuggestionGroupDTO> purchaseSuggestions() {
        syncAutoCompleteFromPurchases();
        List<ReplenishRecord> pending = mapper.selectPendingOrdered();
        if (pending.isEmpty()) {
            return List.of();
        }
        Map<String, LatestPurchaseDTO> latestPurchaseMap = loadLatestPurchases(pending);
        Map<String, List<PurchaseSuggestionItemDTO>> grouped = new LinkedHashMap<>();
        for (ReplenishRecord row : pending) {
            String supplier = StrUtil.blankToDefault(row.getSupplierName(), UNKNOWN_SUPPLIER);
            LatestPurchaseDTO latest = latestPurchaseMap.get(row.getBarcode());
            grouped.computeIfAbsent(supplier, key -> new ArrayList<>())
                    .add(new PurchaseSuggestionItemDTO(
                            row.getId(),
                            row.getBarcode(),
                            row.getNameChinese(),
                            row.getNameForeign(),
                            row.getInventoryQty(),
                            row.getRemark(),
                            latest == null ? null : latest.purchaseDate(),
                            latest == null ? null : latest.supplierName()));
        }
        return grouped.entrySet().stream()
                .map(entry -> new PurchaseSuggestionGroupDTO(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public String exportPurchaseSuggestionsCsv() {
        StringBuilder builder = new StringBuilder("供应商,条码,中文名,外文名,库存,备注,最近采购日期,最近采购供应商\n");
        for (PurchaseSuggestionGroupDTO group : purchaseSuggestions()) {
            for (PurchaseSuggestionItemDTO item : group.items()) {
                builder.append(csv(group.supplierName())).append(',')
                        .append(csvBarcode(item.barcode())).append(',')
                        .append(csv(item.nameChinese())).append(',')
                        .append(csv(item.nameForeign())).append(',')
                        .append(csvDecimal(item.inventoryQty())).append(',')
                        .append(csv(item.remark())).append(',')
                        .append(csv(formatExportDate(item.lastPurchaseDate()))).append(',')
                        .append(csv(item.lastPurchaseSupplier())).append('\n');
            }
        }
        return builder.toString();
    }

    private Map<String, LatestPurchaseDTO> loadLatestPurchases(List<ReplenishRecord> pending) {
        List<String> barcodes = pending.stream().map(ReplenishRecord::getBarcode).distinct().toList();
        List<LatestPurchaseDTO> latestPurchases = talentOposDataSourceService.executeInSession(session -> session
                .getMapper(PosPurchaseMapper.class)
                .selectLatestPurchaseByBarcodes(barcodes));
        return latestPurchases.stream()
                .collect(Collectors.toMap(LatestPurchaseDTO::barcode, Function.identity(), (a, b) -> a));
    }

    private String normalizeStatus(String status) {
        if (StrUtil.isBlank(status)) {
            return null;
        }
        if (STATUS_PENDING.equals(status) || STATUS_DONE.equals(status)) {
            return status;
        }
        return null;
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private String csvBarcode(String barcode) {
        if (StrUtil.isBlank(barcode)) {
            return "";
        }
        String escaped = barcode.trim().replace("\"", "\"\"");
        return "\"\t" + escaped + "\"";
    }

    private String csvDecimal(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return csv(value.stripTrailingZeros().toPlainString());
    }

    private String formatExportDate(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return EXPORT_DATE_FORMAT.format(value);
    }

    private void syncAutoCompleteFromPurchases() {
        List<ReplenishRecord> pending = mapper.selectPendingOrdered();
        if (pending.isEmpty()) {
            return;
        }
        List<String> barcodes =
                pending.stream().map(ReplenishRecord::getBarcode).distinct().toList();
        Map<String, LatestPurchaseDTO> latestFullyReceived = talentOposDataSourceService.executeInSession(
                        session -> session.getMapper(PosPurchaseMapper.class)
                                .selectLatestFullyReceivedPurchaseByBarcodes(barcodes))
                .stream()
                .collect(Collectors.toMap(LatestPurchaseDTO::barcode, Function.identity(), (a, b) -> a));
        for (ReplenishRecord row : pending) {
            LatestPurchaseDTO latest = latestFullyReceived.get(row.getBarcode());
            if (latest == null || latest.purchaseDate() == null || row.getCreatedAt() == null) {
                continue;
            }
            if (latest.purchaseDate().isAfter(row.getCreatedAt())) {
                mapper.updateStatusIfCurrent(row.getId(), STATUS_PENDING, STATUS_DONE);
            }
        }
    }

    private List<ReplenishRecordTreeNodeDTO> buildTree(List<ReplenishRecord> rows) {
        Map<String, List<ReplenishRecordItemDTO>> grouped = new LinkedHashMap<>();
        for (ReplenishRecord row : rows) {
            String supplier = StrUtil.blankToDefault(row.getSupplierName(), UNKNOWN_SUPPLIER);
            grouped.computeIfAbsent(supplier, k -> new ArrayList<>()).add(toItem(row));
        }
        return grouped.entrySet().stream()
                .map(e -> new ReplenishRecordTreeNodeDTO(e.getKey(), e.getValue()))
                .toList();
    }

    private ReplenishRecordItemDTO toItem(ReplenishRecord row) {
        return new ReplenishRecordItemDTO(
                row.getId(),
                row.getBarcode(),
                row.getNameChinese(),
                row.getNameForeign(),
                row.getSupplierName(),
                row.getInventoryQty(),
                row.getRemark(),
                row.getStatus(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }
}
