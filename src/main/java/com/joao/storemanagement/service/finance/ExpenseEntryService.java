package com.joao.storemanagement.service.finance;

import com.joao.storemanagement.dto.finance.ExpenseEntryRequestDTO;
import com.joao.storemanagement.entity.finance.ExpenseEntry;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseEntryService {

    List<ExpenseEntry> list(LocalDate fromDate, LocalDate toDate);

    ExpenseEntry create(ExpenseEntryRequestDTO request);

    ExpenseEntry update(Long id, ExpenseEntryRequestDTO request);

    void delete(Long id);
}
