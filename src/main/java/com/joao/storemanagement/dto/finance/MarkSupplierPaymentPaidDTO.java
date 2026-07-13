package com.joao.storemanagement.dto.finance;

import java.time.LocalDate;

public record MarkSupplierPaymentPaidDTO(LocalDate paidDate, String paymentMethod) {}
