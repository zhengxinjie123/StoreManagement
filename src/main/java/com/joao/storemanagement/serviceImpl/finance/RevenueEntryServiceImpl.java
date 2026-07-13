package com.joao.storemanagement.serviceImpl.finance;

import com.joao.storemanagement.dto.finance.RevenueEntryRequestDTO;
import com.joao.storemanagement.entity.finance.RevenueEntry;
import com.joao.storemanagement.mapper.primary.finance.RevenueEntryMapper;
import com.joao.storemanagement.exception.BusinessException;
import com.joao.storemanagement.service.finance.RevenueEntryService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevenueEntryServiceImpl implements RevenueEntryService {

    private final RevenueEntryMapper mapper;

    public RevenueEntryServiceImpl(RevenueEntryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<RevenueEntry> list(LocalDate fromDate, LocalDate toDate) {
        return mapper.selectByDateRange(fromDate, toDate);
    }

    @Override
    @Transactional
    public RevenueEntry create(RevenueEntryRequestDTO request) {
        RevenueEntry entry = toEntity(request);
        mapper.insert(entry);
        return mapper.selectById(entry.getId());
    }

    @Override
    @Transactional
    public RevenueEntry update(Long id, RevenueEntryRequestDTO request) {
        RevenueEntry existing = mapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("revenue entry not found");
        }
        RevenueEntry entry = toEntity(request);
        entry.setId(id);
        mapper.update(entry);
        return mapper.selectById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (mapper.deleteById(id) == 0) {
            throw new BusinessException("revenue entry not found");
        }
    }

    private RevenueEntry toEntity(RevenueEntryRequestDTO request) {
        RevenueEntry entry = new RevenueEntry();
        entry.setEntryDate(request.entryDate());
        entry.setAmount(request.amount());
        entry.setChannel(request.channel());
        entry.setRemark(request.remark());
        return entry;
    }
}
