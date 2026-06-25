package com.joao.storemanagement.talent.purchase.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.joao.storemanagement.talent.purchase.entity.Tax;
import com.joao.storemanagement.talent.purchase.exception.ImportRowException;
import com.joao.storemanagement.talent.purchase.mapper.PurchaseTaxMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PurchaseTaxRateService {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.0001");

    private final PurchaseTaxMapper taxMapper;

    public PurchaseTaxRateService(PurchaseTaxMapper taxMapper) {
        this.taxMapper = taxMapper;
    }

    public BigDecimal resolveAllowedRate(BigDecimal rate, int rowNumber, String barcode) {
        if (rate == null) {
            throw new ImportRowException(rowNumber, barcode, "税率不能为空");
        }
        List<BigDecimal> allowed = loadAllowedRates();
        for (BigDecimal candidate : allowed) {
            if (rate.subtract(candidate).abs().compareTo(TOLERANCE) <= 0) {
                return candidate;
            }
        }
        throw new ImportRowException(
                rowNumber,
                barcode,
                "税率不对，系统只支持这几种：" + formatAllowedPercents(allowed));
    }

    public BigDecimal defaultRate() {
        List<BigDecimal> allowed = loadAllowedRates();
        for (BigDecimal candidate : allowed) {
            if (candidate.compareTo(new BigDecimal("0.23")) == 0) {
                return candidate;
            }
        }
        return allowed.isEmpty() ? new BigDecimal("0.23") : allowed.get(0);
    }

    private List<BigDecimal> loadAllowedRates() {
        List<Tax> rows = taxMapper.selectList(Wrappers.lambdaQuery(Tax.class).orderByDesc(Tax::getTaxRate));
        if (rows.isEmpty()) {
            return List.of(
                    new BigDecimal("0.23"),
                    new BigDecimal("0.13"),
                    new BigDecimal("0.06"),
                    BigDecimal.ZERO);
        }
        return rows.stream()
                .map(Tax::getTaxRate)
                .filter(Objects::nonNull)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static String formatAllowedPercents(List<BigDecimal> rates) {
        return rates.stream()
                .sorted(Comparator.reverseOrder())
                .map(PurchaseTaxRateService::toPercentLabel)
                .collect(Collectors.joining("、"));
    }

    private static String toPercentLabel(BigDecimal rate) {
        return rate.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP) + "%";
    }
}
