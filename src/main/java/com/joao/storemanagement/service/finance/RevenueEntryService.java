package com.joao.storemanagement.service.finance;

import com.joao.storemanagement.dto.finance.RevenueEntryRequestDTO;
import com.joao.storemanagement.entity.finance.RevenueEntry;
import java.time.LocalDate;
import java.util.List;

public interface RevenueEntryService {

    List<RevenueEntry> list(LocalDate fromDate, LocalDate toDate);

    RevenueEntry create(RevenueEntryRequestDTO request);

    RevenueEntry update(Long id, RevenueEntryRequestDTO request);

    void delete(Long id);
}
