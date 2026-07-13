package com.joao.storemanagement.serviceImpl.pricing;

import cn.hutool.core.util.StrUtil;
import com.joao.storemanagement.dto.pricing.RetailPriceSyncLineDTO;
import com.joao.storemanagement.dto.pricing.RetailPriceSyncPreviewDTO;
import com.joao.storemanagement.dto.talent.ProductDTO;
import com.joao.storemanagement.entity.pricing.RetailPriceRefLine;
import com.joao.storemanagement.entity.pricing.RetailPriceSource;
import com.joao.storemanagement.entity.pricing.RetailPriceSyncRun;
import com.joao.storemanagement.entity.pricing.RetailPriceSyncRunLine;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.mapper.primary.pricing.RetailPriceRefLineMapper;
import com.joao.storemanagement.mapper.primary.pricing.RetailPriceSourceMapper;
import com.joao.storemanagement.mapper.primary.pricing.RetailPriceSyncRunLineMapper;
import com.joao.storemanagement.mapper.primary.pricing.RetailPriceSyncRunMapper;
import com.joao.storemanagement.mapper.talent.PosProductPriceWriteMapper;
import com.joao.storemanagement.service.pricing.RetailPriceSyncService;
import com.joao.storemanagement.service.talent.TalentOposDataSourceService;
import com.joao.storemanagement.utils.BarcodeUtil;
import com.joao.storemanagement.vo.pricing.RetailPriceSyncRunLineVO;
import com.joao.storemanagement.vo.pricing.RetailPriceSyncRunVO;
import com.joao.storemanagement.vo.response.PageResponseVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RetailPriceSyncServiceImpl implements RetailPriceSyncService {

    private static final long REF_CACHE_TTL_MS = 120_000L;
    private static final String REF_TEMP_TABLE = "#RetailPriceRefBarcodes";

    private record RefHit(
            String sourceCode, String sourceName, BigDecimal purchasePriceTax, BigDecimal retailPriceTax) {}

    private record RefSnapshot(Map<String, RefHit> refMap, List<String> barcodes) {}

    private record PreviewQueryResult(long totalMissing, long matchedCount, List<ProductDTO> products) {}

    private record ApplyOutcome(int matched, int applied, int skipped, List<RetailPriceSyncRunLine> appliedLines) {}

    private final RetailPriceSourceMapper sourceMapper;
    private final RetailPriceRefLineMapper refLineMapper;
    private final TalentOposDataSourceService talentOposDataSourceService;
    private final RetailPriceSyncRunMapper syncRunMapper;
    private final RetailPriceSyncRunLineMapper syncRunLineMapper;
    private final RetailPriceSyncRunLineBatchInserter syncRunLineBatchInserter;

    private volatile RefSnapshot cachedRefSnapshot;
    private volatile long cachedRefSnapshotAt;

    public RetailPriceSyncServiceImpl(
            RetailPriceSourceMapper sourceMapper,
            RetailPriceRefLineMapper refLineMapper,
            TalentOposDataSourceService talentOposDataSourceService,
            RetailPriceSyncRunMapper syncRunMapper,
            RetailPriceSyncRunLineMapper syncRunLineMapper,
            RetailPriceSyncRunLineBatchInserter syncRunLineBatchInserter) {
        this.sourceMapper = sourceMapper;
        this.refLineMapper = refLineMapper;
        this.talentOposDataSourceService = talentOposDataSourceService;
        this.syncRunMapper = syncRunMapper;
        this.syncRunLineMapper = syncRunLineMapper;
        this.syncRunLineBatchInserter = syncRunLineBatchInserter;
    }

    @Override
    public RetailPriceSyncPreviewDTO preview(long current, long pageSize, String status) {
        long safeCurrent = Math.max(current, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 200);
        String statusFilter = normalizeStatusFilter(status);
        RefSnapshot refSnapshot = refSnapshot();
        long offset = (safeCurrent - 1) * safePageSize;

        PreviewQueryResult queryResult = queryPreviewPage(refSnapshot.barcodes(), statusFilter, offset, safePageSize);
        long totalMissing = queryResult.totalMissing();
        long matchedCount = queryResult.matchedCount();
        long noRefCount = Math.max(totalMissing - matchedCount, 0);
        long total =
                switch (statusFilter) {
                    case "matched" -> matchedCount;
                    case "no_ref" -> noRefCount;
                    default -> totalMissing;
                };

        List<RetailPriceSyncLineDTO> lines =
                queryResult.products().stream().map(product -> toSyncLine(product, refSnapshot.refMap())).toList();
        return new RetailPriceSyncPreviewDTO(
                (int) totalMissing,
                (int) matchedCount,
                (int) noRefCount,
                safeCurrent,
                safePageSize,
                total,
                lines);
    }

    @Override
    public List<String> matchedGuids() {
        RefSnapshot refSnapshot = refSnapshot();
        return withRefTempTable(refSnapshot.barcodes(), PosProductPriceWriteMapper::selectMissingRetailMatchedGuidsWithTemp);
    }

    private RefSnapshot refSnapshot() {
        long now = System.currentTimeMillis();
        RefSnapshot snapshot = cachedRefSnapshot;
        if (snapshot != null && now - cachedRefSnapshotAt < REF_CACHE_TTL_MS) {
            return snapshot;
        }
        synchronized (this) {
            snapshot = cachedRefSnapshot;
            if (snapshot != null && now - cachedRefSnapshotAt < REF_CACHE_TTL_MS) {
                return snapshot;
            }
            Map<String, RefHit> refMap = buildRefMap();
            snapshot = new RefSnapshot(refMap, List.copyOf(refMap.keySet()));
            cachedRefSnapshot = snapshot;
            cachedRefSnapshotAt = now;
            return snapshot;
        }
    }

    private void invalidateRefSnapshot() {
        cachedRefSnapshot = null;
        cachedRefSnapshotAt = 0L;
    }

    private String normalizeStatusFilter(String status) {
        if ("matched".equals(status) || "no_ref".equals(status)) {
            return status;
        }
        return "all";
    }

    private PreviewQueryResult queryPreviewPage(
            List<String> refBarcodes, String statusFilter, long offset, long pageSize) {
        return withRefTempTable(refBarcodes, mapper -> {
            long totalMissing = mapper.countMissingRetailPrice();
            long matchedCount = mapper.countMissingRetailMatchedWithTemp();
            List<ProductDTO> products =
                    switch (statusFilter) {
                        case "matched" -> mapper.selectMissingRetailPricePageMatchedWithTemp(offset, pageSize);
                        case "no_ref" -> mapper.selectMissingRetailPricePageNoRefWithTemp(offset, pageSize);
                        default -> mapper.selectMissingRetailPricePage(offset, pageSize);
                    };
            return new PreviewQueryResult(totalMissing, matchedCount, products);
        });
    }

    private <T> T withRefTempTable(List<String> refBarcodes, Function<PosProductPriceWriteMapper, T> action) {
        return talentOposDataSourceService.executeInSession(session -> {
            try {
                Connection connection = session.getConnection();
                createRefTempTable(connection);
                insertRefTempBarcodes(connection, refBarcodes);
                return action.apply(session.getMapper(PosProductPriceWriteMapper.class));
            } catch (SQLException ex) {
                throw new BusinessException("预览匹配查询失败，请稍后重试");
            } finally {
                dropRefTempTableQuietly(session.getConnection());
            }
        });
    }

    private void createRefTempTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE " + REF_TEMP_TABLE + " (barcode nvarchar(128) NOT NULL PRIMARY KEY)");
        }
    }

    private void insertRefTempBarcodes(Connection connection, List<String> refBarcodes) throws SQLException {
        if (refBarcodes.isEmpty()) {
            return;
        }
        try (PreparedStatement ps =
                connection.prepareStatement("INSERT INTO " + REF_TEMP_TABLE + " (barcode) VALUES (?)")) {
            int batchSize = 0;
            for (String barcode : refBarcodes) {
                ps.setString(1, barcode);
                ps.addBatch();
                batchSize++;
                if (batchSize >= 500) {
                    ps.executeBatch();
                    batchSize = 0;
                }
            }
            if (batchSize > 0) {
                ps.executeBatch();
            }
        }
    }

    private void dropRefTempTableQuietly(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            statement.execute("IF OBJECT_ID('tempdb.." + REF_TEMP_TABLE + "') IS NOT NULL DROP TABLE " + REF_TEMP_TABLE);
        } catch (SQLException ignored) {
            // Temporary table cleanup should not hide the real query result.
        }
    }

    @Override
    @Transactional
    public Map<String, Object> apply(List<String> productGuids) {
        if (productGuids == null || productGuids.isEmpty()) {
            throw new BusinessException("请选择要同步的商品");
        }
        Set<String> selected = new HashSet<>(productGuids);
        ApplyOutcome outcome = applyToPos(selected);
        // 记录同步批次并写入明细
        RetailPriceSyncRun run = new RetailPriceSyncRun();
        run.setMatchedCount(outcome.matched());
        run.setAppliedCount(outcome.applied());
        run.setSkippedCount(outcome.skipped());
        run.setRemark("sync selected retail from ref sources, count=" + selected.size());
        syncRunMapper.insert(run);
        persistRunLines(run.getId(), outcome.appliedLines());
        invalidateRefSnapshot();
        return Map.of(
                "runId", run.getId(),
                "matched", outcome.matched(),
                "applied", outcome.applied(),
                "skipped", outcome.skipped(),
                "selected", selected.size());
    }

    @Override
    public PageResponseVO<RetailPriceSyncRunVO> syncHistory(long current, long pageSize) {
        long safeCurrent = Math.max(current, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 200);
        long offset = (safeCurrent - 1) * safePageSize;
        long total = syncRunMapper.countAll();
        List<RetailPriceSyncRunVO> records = syncRunMapper.selectPage(offset, safePageSize).stream()
                .map(RetailPriceSyncRunVO::of)
                .toList();
        return PageResponseVO.of(safeCurrent, safePageSize, total, records);
    }

    @Override
    public PageResponseVO<RetailPriceSyncRunLineVO> syncRunLines(Long runId, long current, long pageSize) {
        requireRunExists(runId);
        long safeCurrent = Math.max(current, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 200);
        long offset = (safeCurrent - 1) * safePageSize;
        long total = syncRunLineMapper.countByRunId(runId);
        List<RetailPriceSyncRunLineVO> records = syncRunLineMapper.selectPageByRunId(runId, offset, safePageSize)
                .stream()
                .map(RetailPriceSyncRunLineVO::of)
                .toList();
        return PageResponseVO.of(safeCurrent, safePageSize, total, records);
    }

    @Override
    public List<RetailPriceSyncRunLineVO> rollbackPreview(Long runId) {
        RetailPriceSyncRun run = requireRollbackableRun(runId);
        return syncRunLineMapper.selectByRunId(run.getId()).stream()
                .map(RetailPriceSyncRunLineVO::of)
                .toList();
    }

    @Override
    @Transactional
    public Map<String, Object> rollback(Long runId) {
        RetailPriceSyncRun run = requireRollbackableRun(runId);
        List<RetailPriceSyncRunLine> lines = syncRunLineMapper.selectByRunId(run.getId());
        if (lines.isEmpty()) {
            throw new BusinessException("该同步批次没有可回滚的明细");
        }
        int restored = restorePrices(lines);
        RetailPriceSyncRun rollbackRun = new RetailPriceSyncRun();
        rollbackRun.setMatchedCount(lines.size());
        rollbackRun.setAppliedCount(restored);
        rollbackRun.setSkippedCount(lines.size() - restored);
        rollbackRun.setRollbackOfRunId(run.getId());
        rollbackRun.setRemark("rollback sync run #" + run.getId());
        syncRunMapper.insert(rollbackRun);
        return Map.of(
                "rollbackRunId", rollbackRun.getId(),
                "sourceRunId", run.getId(),
                "restored", restored,
                "skipped", lines.size() - restored);
    }

    private ApplyOutcome applyToPos(Set<String> selectedGuids) {
        Map<String, RefHit> refMap = buildRefMap();
        List<ProductDTO> products = selectMissingRetailPrice();
        int[] matched = {0};
        int[] applied = {0};
        int[] skipped = {0};
        List<RetailPriceSyncRunLine> appliedLines = new ArrayList<>();

        talentOposDataSourceService.executeInWriteTransaction(session -> {
            PosProductPriceWriteMapper mapper = session.getMapper(PosProductPriceWriteMapper.class);
            for (ProductDTO product : products) {
                if (!selectedGuids.contains(product.guid())) {
                    continue;
                }
                RetailPriceSyncLineDTO line = toSyncLine(product, refMap);
                if (!"matched".equals(line.status())) {
                    skipped[0]++;
                    continue;
                }
                matched[0]++;
                BigDecimal retailPriceTax = line.refRetailPriceTax();
                BigDecimal retailPrice = calcPriceExTax(retailPriceTax, product.taxRate());
                int updated = mapper.updateRetailPrice(product.guid(), retailPriceTax, retailPrice);
                if (updated > 0) {
                    applied[0]++;
                    appliedLines.add(toRunLine(null, product, line));
                } else {
                    skipped[0]++;
                }
            }
            return null;
        });

        return new ApplyOutcome(matched[0], applied[0], skipped[0], appliedLines);
    }

    private int restorePrices(List<RetailPriceSyncRunLine> lines) {
        int[] restored = {0};
        talentOposDataSourceService.executeInWriteTransaction(session -> {
            PosProductPriceWriteMapper mapper = session.getMapper(PosProductPriceWriteMapper.class);
            for (RetailPriceSyncRunLine line : lines) {
                ProductDTO product = mapper.selectByGuid(line.getProductGuid());
                if (product == null) {
                    continue;
                }
                BigDecimal retailPriceTax = line.getOldRetailPriceTax();
                BigDecimal retailPrice = retailPriceTax == null || retailPriceTax.compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : calcPriceExTax(retailPriceTax, product.taxRate());
                if (mapper.updateRetailPriceDirect(line.getProductGuid(), retailPriceTax, retailPrice) > 0) {
                    restored[0]++;
                }
            }
            return null;
        });
        return restored[0];
    }

    private void persistRunLines(Long runId, List<RetailPriceSyncRunLine> lines) {
        for (RetailPriceSyncRunLine line : lines) {
            line.setRunId(runId);
        }
        syncRunLineBatchInserter.insertAll(lines);
    }

    private RetailPriceSyncRunLine toRunLine(Long runId, ProductDTO product, RetailPriceSyncLineDTO line) {
        RetailPriceSyncRunLine row = new RetailPriceSyncRunLine();
        row.setRunId(runId);
        row.setProductGuid(product.guid());
        row.setBarcode(product.barcode());
        row.setOldRetailPriceTax(product.retailPrice());
        row.setNewRetailPriceTax(line.refRetailPriceTax());
        row.setSourceCode(line.matchedSourceCode());
        row.setSourceName(line.matchedSourceName());
        return row;
    }

    private RetailPriceSyncRun requireRunExists(Long runId) {
        RetailPriceSyncRun run = syncRunMapper.selectById(runId);
        if (run == null) {
            throw new BusinessException("同步批次不存在");
        }
        return run;
    }

    private RetailPriceSyncRun requireRollbackableRun(Long runId) {
        RetailPriceSyncRun run = requireRunExists(runId);
        if (run.getRollbackOfRunId() != null) {
            throw new BusinessException("回滚记录不能再次回滚");
        }
        if (syncRunMapper.countRollbackBySourceRunId(runId) > 0) {
            throw new BusinessException("该同步批次已回滚");
        }
        return run;
    }

    private List<ProductDTO> selectMissingRetailPrice() {
        return talentOposDataSourceService.executeInSession(
                session -> session.getMapper(PosProductPriceWriteMapper.class).selectMissingRetailPrice());
    }

    private Map<String, RefHit> buildRefMap() {
        Map<Long, RetailPriceSource> sources = new HashMap<>();
        for (RetailPriceSource source : sourceMapper.selectAllActiveOrdered()) {
            sources.put(source.getId(), source);
        }
        Map<String, RefHit> refMap = new LinkedHashMap<>();
        for (RetailPriceRefLine line : refLineMapper.selectAllFromActiveSourcesOrdered()) {
            String barcode = BarcodeUtil.judgeBarcode(line.getBarcode());
            if (StrUtil.isBlank(barcode) || refMap.containsKey(barcode)) {
                continue;
            }
            RetailPriceSource source = sources.get(line.getSourceId());
            if (source == null) {
                continue;
            }
            refMap.put(
                    barcode,
                    new RefHit(
                            source.getCode(),
                            source.getName(),
                            line.getPurchasePriceTax(),
                            line.getRetailPrice()));
        }
        return refMap;
    }

    private RetailPriceSyncLineDTO toSyncLine(ProductDTO product, Map<String, RefHit> refMap) {
        String barcode = BarcodeUtil.judgeBarcode(product.barcode());
        RefHit hit = refMap.get(barcode);
        if (hit == null) {
            return new RetailPriceSyncLineDTO(
                    product.guid(),
                    product.barcode(),
                    product.productCode(),
                    product.supplierName(),
                    product.nameChinese(),
                    product.nameForeign(),
                    product.purchasePriceTax(),
                    null,
                    null,
                    null,
                    null,
                    "no_ref");
        }
        return new RetailPriceSyncLineDTO(
                product.guid(),
                product.barcode(),
                product.productCode(),
                product.supplierName(),
                product.nameChinese(),
                product.nameForeign(),
                product.purchasePriceTax(),
                hit.purchasePriceTax(),
                hit.retailPriceTax(),
                hit.sourceCode(),
                hit.sourceName(),
                "matched");
    }

    private BigDecimal calcPriceExTax(BigDecimal priceTax, BigDecimal taxRate) {
        BigDecimal rate = taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0
                ? taxRate
                : new BigDecimal("0.23");
        return priceTax.divide(BigDecimal.ONE.add(rate), 4, RoundingMode.HALF_UP);
    }
}
