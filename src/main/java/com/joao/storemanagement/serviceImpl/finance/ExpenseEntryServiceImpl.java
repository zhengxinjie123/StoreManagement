package com.joao.storemanagement.serviceImpl.finance;

import com.joao.storemanagement.dto.finance.ExpenseEntryRequestDTO;
import com.joao.storemanagement.entity.finance.ExpenseEntry;
import com.joao.storemanagement.mapper.primary.finance.ExpenseEntryMapper;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.service.finance.ExpenseEntryService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseEntryServiceImpl implements ExpenseEntryService {

    private final ExpenseEntryMapper mapper;

    public ExpenseEntryServiceImpl(ExpenseEntryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<ExpenseEntry> list(LocalDate fromDate, LocalDate toDate) {
        return mapper.selectByDateRange(fromDate, toDate);
    }

    @Override
    @Transactional
    public ExpenseEntry create(ExpenseEntryRequestDTO request) {
        ExpenseEntry entry = toEntity(request);
        mapper.insert(entry);
        return mapper.selectById(entry.getId());
    }

    @Override
    @Transactional
    public ExpenseEntry update(Long id, ExpenseEntryRequestDTO request) {
        if (mapper.selectById(id) == null) {
            throw new BusinessException("expense entry not found");
        }
        ExpenseEntry entry = toEntity(request);
        entry.setId(id);
        mapper.update(entry);
        return mapper.selectById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (mapper.deleteById(id) == 0) {
            throw new BusinessException("expense entry not found");
        }
    }

    private ExpenseEntry toEntity(ExpenseEntryRequestDTO request) {
        ExpenseEntry entry = new ExpenseEntry();
        entry.setEntryDate(request.entryDate());
        entry.setAmount(request.amount());
        entry.setCategory(request.category());
        entry.setProxyPayment(false);
        entry.setRemark(request.remark());
        return entry;
    }
}
