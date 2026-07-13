package com.joao.storemanagement.service.finance;

import com.joao.storemanagement.dto.finance.MarkSupplierPaymentPaidDTO;
import com.joao.storemanagement.dto.finance.SupplierPaymentPendingRequestDTO;
import com.joao.storemanagement.entity.finance.SupplierPaymentPending;
import com.joao.storemanagement.vo.response.PageResponseVO;
import java.time.LocalDate;

public interface SupplierPaymentPendingService {

    /**
     * 分页查询供应商待付款
     *
     * @param current       页码，从 1 开始
     * @param pageSize      每页条数
     * @param status        付款状态
     * @param supplierName  供应商名称
     * @param dueFrom       付款期限开始日期
     * @param dueTo         付款期限结束日期
     * @param overdueStatus 逾期状态
     * @return 分页结果
     */
    PageResponseVO<SupplierPaymentPending> page(
            long current,
            long pageSize,
            String status,
            String supplierName,
            LocalDate dueFrom,
            LocalDate dueTo,
            String overdueStatus);

    SupplierPaymentPending create(SupplierPaymentPendingRequestDTO request);

    SupplierPaymentPending update(Long id, SupplierPaymentPendingRequestDTO request);

    /**
     * 标记供应商货款已结清
     *
     * @param id      待付款记录 ID
     * @param request 付款信息
     * @return 更新后的记录
     */
    SupplierPaymentPending markPaid(Long id, MarkSupplierPaymentPaidDTO request);

    /**
     * 撤销付款，恢复为待付款状态
     *
     * @param id 待付款记录 ID
     * @return 更新后的记录
     */
    SupplierPaymentPending reopen(Long id);

    void delete(Long id);
}
