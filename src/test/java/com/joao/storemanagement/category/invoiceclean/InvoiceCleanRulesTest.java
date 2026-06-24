package com.joao.storemanagement.category.invoiceclean;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceCleanRulesTest {

    @Test
    void lineSubtotalColumnFollowsTemplateTaxIncludedMode() {
        assertThat(InvoiceCleanRules.lineSubtotalColumnIsTaxIncluded(true)).isTrue();
        assertThat(InvoiceCleanRules.lineSubtotalColumnIsTaxIncluded(false)).isFalse();
    }
}
