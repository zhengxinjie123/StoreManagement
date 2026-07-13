package com.joao.storemanagement.service.finance;

import com.joao.storemanagement.dto.finance.PaymentDueTermRequestDTO;
import com.joao.storemanagement.entity.finance.PaymentDueTerm;
import java.time.LocalDate;
import java.util.List;

public interface PaymentDueTermService {

    List<PaymentDueTerm> listActive();

    /**
     * 查询全部付款期限配置，含停用项。
     *
     * @return 付款期限列表
     */
    List<PaymentDueTerm> listAll();

    LocalDate resolveDueDate(LocalDate arrivalDate, String termCode);

    String resolveTermLabel(String termCode);

    PaymentDueTerm update(Long id, PaymentDueTermRequestDTO request);
}
