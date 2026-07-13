package com.joao.storemanagement.dto.replenish;

import jakarta.validation.constraints.Size;

public record ReplenishRemarkUpdateDTO(@Size(max = 500) String remark) {}
