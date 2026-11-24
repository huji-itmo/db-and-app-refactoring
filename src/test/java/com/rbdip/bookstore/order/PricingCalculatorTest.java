package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PricingCalculatorTest {

    private final PricingCalculator calculator = new PricingCalculator();

    @ParameterizedTest
    @CsvSource(value = {
        "20, 10, regular, NULL, 200.00",
        "20, 11, regular, NULL, 209.00",
        "1000, 1, regular, NULL, 1000.00",
        "1000.01, 1, regular, NULL, 980.01",
        "1010, 1, regular, SAVE10, 1000.00",
        "100, 1, vip, SAVE10, 80.00",
        "100, 1, wholesale, SAVE20PERCENT, 68.00",
        "5, 1, regular, SAVE10, 0.00",
        "-5, 1, regular, NULL, 0.00",
        "5, -1, regular, NULL, 0.00",
        "5, 0, regular, NULL, 0.00",
        "1.005, 1, NULL, UNKNOWN, 1.01"
    }, nullValues = "NULL")
    void preservesExistingPricingRules(
            BigDecimal price, int quantity, String customerType, String coupon, BigDecimal expected) {
        var items = List.of(new PricingCalculator.LineItem(price, quantity));

        assertThat(calculator.calculateOrderTotal(items, customerType, coupon)).isEqualTo(expected);
    }

    @Test
    void returnsZeroForAnEmptyOrder() {
        assertThat(calculator.calculateOrderTotal(List.of(), "regular", null))
                .isEqualTo(new BigDecimal("0.00"));
    }
}
