package com.joao.storemanagement.dto.pricing;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record RetailPriceManageUpdateDTO(@NotNull @Positive BigDecimal retailPriceTax) {}
