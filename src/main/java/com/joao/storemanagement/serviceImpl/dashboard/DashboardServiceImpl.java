package com.joao.storemanagement.serviceImpl.dashboard;

import com.joao.storemanagement.dto.dashboard.DashboardQuickLinkDTO;
import com.joao.storemanagement.dto.dashboard.DashboardSummaryDTO;
import com.joao.storemanagement.mapper.primary.DashboardMapper;
import com.joao.storemanagement.mapper.talent.PosProductPriceWriteMapper;
import com.joao.storemanagement.repository.ProductRepository;
import com.joao.storemanagement.service.dashboard.DashboardService;
import com.joao.storemanagement.service.talent.TalentOposDataSourceService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {

    private static final BigDecimal DEFAULT_ALERT_MIN = new BigDecimal("0.01");

    private final DashboardMapper dashboardMapper;
    private final ProductRepository productRepository;
    private final TalentOposDataSourceService talentOposDataSourceService;

    public DashboardServiceImpl(
            DashboardMapper dashboardMapper,
            ProductRepository productRepository,
            TalentOposDataSourceService talentOposDataSourceService) {
        this.dashboardMapper = dashboardMapper;
        this.productRepository = productRepository;
        this.talentOposDataSourceService = talentOposDataSourceService;
    }

    @Override
    public DashboardSummaryDTO summary() {
        long todayUploaded = dashboardMapper.countTodayUploadedInvoices();
        long pendingClean = dashboardMapper.countPendingCleanInvoices();
        long importFailed = dashboardMapper.countImportFailedInvoices();
        BigDecimal pendingPaymentAmount = zero(dashboardMapper.sumPendingPaymentAmount());
        long overduePayments = dashboardMapper.countOverduePayments();
        long pendingReplenish = dashboardMapper.countPendingReplenishRecords();
        long pendingRetailSync = countMissingRetailPriceQuietly();
        long priceAlerts = countPriceAlertsQuietly();
        return new DashboardSummaryDTO(
                todayUploaded,
                pendingClean,
                importFailed,
                pendingPaymentAmount,
                overduePayments,
                pendingReplenish,
                pendingRetailSync,
                priceAlerts,
                buildQuickLinks(
                        pendingClean,
                        importFailed,
                        overduePayments,
                        pendingReplenish,
                        pendingRetailSync,
                        priceAlerts));
    }

    private List<DashboardQuickLinkDTO> buildQuickLinks(
            long pendingClean,
            long importFailed,
            long overduePayments,
            long pendingReplenish,
            long pendingRetailSync,
            long priceAlerts) {
        List<DashboardQuickLinkDTO> links = new ArrayList<>();
        if (pendingClean > 0) {
            links.add(new DashboardQuickLinkDTO(
                    "待清洗发票 (" + pendingClean + ")",
                    "/invoice",
                    Map.of("cleanStatus", "NOT_CLEANED")));
        }
        if (importFailed > 0) {
            links.add(new DashboardQuickLinkDTO(
                    "导入失败 (" + importFailed + ")",
                    "/invoice/archive",
                    Map.of("ownerType", "SELF", "importStatus", "FAILED")));
        }
        if (overduePayments > 0) {
            links.add(new DashboardQuickLinkDTO(
                    "逾期付款 (" + overduePayments + ")",
                    "/finance",
                    Map.of("tab", "supplier-payments", "overdueStatus", "overdue")));
        }
        if (pendingReplenish > 0) {
            links.add(new DashboardQuickLinkDTO(
                    "待补货 (" + pendingReplenish + ")",
                    "/replenish",
                    Map.of()));
        }
        if (pendingRetailSync > 0) {
            links.add(new DashboardQuickLinkDTO(
                    "缺零售价 (" + pendingRetailSync + ")",
                    "/pricing/manage",
                    Map.of()));
        }
        if (priceAlerts > 0) {
            links.add(new DashboardQuickLinkDTO(
                    "进价预警 (" + priceAlerts + ")",
                    "/analytics",
                    Map.of()));
        }
        if (!links.isEmpty()) {
            return links;
        }
        return List.of(
                new DashboardQuickLinkDTO("电子发票", "/invoice", Map.of()),
                new DashboardQuickLinkDTO("财务管理", "/finance", Map.of()),
                new DashboardQuickLinkDTO("补货记录", "/replenish", Map.of()),
                new DashboardQuickLinkDTO("外部售价参考", "/pricing", Map.of()),
                new DashboardQuickLinkDTO("售价管理", "/pricing/manage", Map.of()),
                new DashboardQuickLinkDTO("进价分析", "/analytics", Map.of()));
    }

    private long countMissingRetailPriceQuietly() {
        try {
            return talentOposDataSourceService.executeInSession(
                    session -> session.getMapper(PosProductPriceWriteMapper.class).countMissingRetailPrice());
        } catch (RuntimeException ex) {
            log.warn("统计缺零售价失败，回退为 0", ex);
            return 0L;
        }
    }

    private long countPriceAlertsQuietly() {
        try {
            return productRepository.countPriceIncreaseAlerts(
                    DEFAULT_ALERT_MIN, null, null, null, null);
        } catch (RuntimeException ex) {
            log.warn("统计进价预警失败，回退为 0", ex);
            return 0L;
        }
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
