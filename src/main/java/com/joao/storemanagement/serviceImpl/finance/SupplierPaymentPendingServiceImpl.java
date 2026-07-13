package com.joao.storemanagement.serviceImpl.finance;

import cn.hutool.core.util.StrUtil;
import com.joao.storemanagement.dto.finance.MarkSupplierPaymentPaidDTO;
import com.joao.storemanagement.dto.finance.SupplierPaymentPendingRequestDTO;
import com.joao.storemanagement.entity.finance.SupplierPaymentPending;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.mapper.primary.finance.SupplierPaymentPendingMapper;
import com.joao.storemanagement.service.finance.PaymentDueTermService;
import com.joao.storemanagement.service.finance.SupplierPaymentPendingService;
import com.joao.storemanagement.vo.response.PageResponseVO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierPaymentPendingServiceImpl implements SupplierPaymentPendingService {

    private final SupplierPaymentPendingMapper mapper;
    private final PaymentDueTermService paymentDueTermService;

    public SupplierPaymentPendingServiceImpl(
            SupplierPaymentPendingMapper mapper, PaymentDueTermService paymentDueTermService) {
        this.mapper = mapper;
        this.paymentDueTermService = paymentDueTermService;
    }

    @Override
    public PageResponseVO<SupplierPaymentPending> page(
            long current,
            long pageSize,
            String status,
            String supplierName,
            LocalDate dueFrom,
            LocalDate dueTo,
            String overdueStatus) {
        long safeCurrent = Math.max(current, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 200);
        String normalizedStatus = normalizeStatus(status);
        String normalizedSupplierName = StrUtil.trimToNull(supplierName);
        String normalizedOverdueStatus = normalizeOverdueStatus(overdueStatus);
        long offset = (safeCurrent - 1) * safePageSize;
        // 统计符合条件的供应商待付款总数
        long total = mapper.countPage(
                normalizedStatus, normalizedSupplierName, dueFrom, dueTo, normalizedOverdueStatus);
        // 分页查询供应商待付款列表
        List<SupplierPaymentPending> records = mapper.selectPage(
                normalizedStatus,
                normalizedSupplierName,
                dueFrom,
                dueTo,
                normalizedOverdueStatus,
                offset,
                safePageSize);
        return PageResponseVO.of(safeCurrent, safePageSize, total, records);
    }

    @Override
    @Transactional
    public SupplierPaymentPending create(SupplierPaymentPendingRequestDTO request) {
        if (StrUtil.isBlank(request.paymentTermCode())) {
            throw new BusinessException("请选择付款期限");
        }
        if (request.amount() == null) {
            throw new BusinessException("请填写金额");
        }
        SupplierPaymentPending row = toEntity(null, request);
        row.setStatus(request.status() != null ? request.status() : "open");
        mapper.insert(row);
        return mapper.selectById(row.getId());
    }

    @Override
    @Transactional
    public SupplierPaymentPending update(Long id, SupplierPaymentPendingRequestDTO request) {
        if (mapper.selectById(id) == null) {
            throw new BusinessException("supplier payment not found");
        }
        SupplierPaymentPending row = toEntity(id, request);
        row.setId(id);
        row.setStatus(request.status() != null ? request.status() : "open");
        mapper.update(row);
        return mapper.selectById(id);
    }

    @Override
    @Transactional
    public SupplierPaymentPending markPaid(Long id, MarkSupplierPaymentPaidDTO request) {
        SupplierPaymentPending existing = mapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("supplier payment not found");
        }
        if ("paid".equals(existing.getStatus())) {
            throw new BusinessException("该货款已结清");
        }
        existing.setStatus("paid");
        existing.setPaidDate(request != null && request.paidDate() != null ? request.paidDate() : LocalDate.now());
        if (request != null && StrUtil.isNotBlank(request.paymentMethod())) {
            existing.setPaymentMethod(request.paymentMethod());
        }
        mapper.update(existing);
        return mapper.selectById(id);
    }

    @Override
    @Transactional
    public SupplierPaymentPending reopen(Long id) {
        SupplierPaymentPending existing = mapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("supplier payment not found");
        }
        if (!"paid".equals(existing.getStatus())) {
            throw new BusinessException("仅已结清记录可撤销付款");
        }
        existing.setStatus("open");
        existing.setPaidDate(null);
        mapper.update(existing);
        return mapper.selectById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (mapper.deleteById(id) == 0) {
            throw new BusinessException("supplier payment not found");
        }
    }

    private SupplierPaymentPending toEntity(Long id, SupplierPaymentPendingRequestDTO request) {
        SupplierPaymentPending row = new SupplierPaymentPending();
        row.setSupplierName(request.supplierName());
        row.setOrderDate(request.orderDate());
        row.setArrivalDate(request.arrivalDate());
        row.setPaymentMethod(request.paymentMethod());
        if (StrUtil.isNotBlank(request.paymentTermCode())) {
            row.setPaymentTermCode(request.paymentTermCode());
            row.setPaymentTerm(paymentDueTermService.resolveTermLabel(request.paymentTermCode()));
            row.setPaymentDueDate(
                    paymentDueTermService.resolveDueDate(request.arrivalDate(), request.paymentTermCode()));
        } else if (id != null) {
            SupplierPaymentPending existing = mapper.selectById(id);
            row.setPaymentTermCode(existing.getPaymentTermCode());
            row.setPaymentTerm(existing.getPaymentTerm());
            row.setPaymentDueDate(existing.getPaymentDueDate());
        }
        row.setAmount(request.amount());
        row.setRemark(request.remark());
        row.setPaidDate(request.paidDate());
        return row;
    }

    private String normalizeStatus(String status) {
        if (StrUtil.isBlank(status)) {
            return null;
        }
        if ("open".equals(status) || "paid".equals(status)) {
            return status;
        }
        return null;
    }

    private String normalizeOverdueStatus(String overdueStatus) {
        if (StrUtil.isBlank(overdueStatus)) {
            return null;
        }
        if ("overdue".equals(overdueStatus)
                || "due_soon".equals(overdueStatus)
                || "unknown".equals(overdueStatus)) {
            return overdueStatus;
        }
        return null;
    }
}
