package com.joao.storemanagement.serviceImpl.finance;

import com.joao.storemanagement.dto.finance.PaymentDueTermRequestDTO;
import com.joao.storemanagement.entity.finance.PaymentDueTerm;
import com.joao.storemanagement.mapper.primary.finance.PaymentDueTermMapper;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.service.finance.PaymentDueTermService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentDueTermServiceImpl implements PaymentDueTermService {

    private final PaymentDueTermMapper mapper;

    public PaymentDueTermServiceImpl(PaymentDueTermMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<PaymentDueTerm> listActive() {
        return mapper.selectActiveOrdered();
    }

    @Override
    public List<PaymentDueTerm> listAll() {
        return mapper.selectAllOrdered();
    }

    @Override
    public LocalDate resolveDueDate(LocalDate arrivalDate, String termCode) {
        PaymentDueTerm term = requireTerm(termCode);
        if (Boolean.TRUE.equals(term.getUnknownTerm())) {
            return null;
        }
        if (arrivalDate == null) {
            throw new BusinessException("请选择到货日期以推算付款期限");
        }
        if (term.getOffsetMonths() != null) {
            return arrivalDate.plusMonths(term.getOffsetMonths());
        }
        if (term.getOffsetDays() != null) {
            return arrivalDate.plusDays(term.getOffsetDays());
        }
        throw new BusinessException("付款期限配置无效: " + termCode);
    }

    @Override
    public String resolveTermLabel(String termCode) {
        return requireTerm(termCode).getLabel();
    }

    @Override
    @Transactional
    public PaymentDueTerm update(Long id, PaymentDueTermRequestDTO request) {
        PaymentDueTerm existing = mapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("付款期限配置不存在");
        }
        existing.setLabel(request.label());
        existing.setOffsetDays(request.offsetDays());
        existing.setOffsetMonths(request.offsetMonths());
        existing.setUnknownTerm(request.unknownTerm());
        existing.setSortOrder(request.sortOrder());
        existing.setActive(request.active());
        mapper.update(existing);
        return mapper.selectById(id);
    }

    private PaymentDueTerm requireTerm(String termCode) {
        PaymentDueTerm term = mapper.selectByCode(termCode);
        if (term == null) {
            throw new BusinessException("付款期限选项无效: " + termCode);
        }
        return term;
    }
}
