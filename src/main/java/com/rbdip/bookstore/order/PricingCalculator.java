package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Модуль расчёта цены заказа. Намеренно почти не покрыт тестами и
 * содержит magic numbers / нечитаемые ветвления скидок - цель для
 * характеризационных тестов (ЛР2) и mutation-testing гейта PIT (ЛР5).
 */
public class PricingCalculator {

    private static final int BULK_DISCOUNT_THRESHOLD = 10;
    private static final BigDecimal BULK_DISCOUNT_RATE = new BigDecimal("0.95");
    private static final BigDecimal VIP_DISCOUNT_RATE = new BigDecimal("0.9");
    private static final BigDecimal WHOLESALE_DISCOUNT_RATE = new BigDecimal("0.85");
    private static final BigDecimal SAVE10_COUPON_VALUE = new BigDecimal("10");
    private static final BigDecimal SAVE20_DISCOUNT_RATE = new BigDecimal("0.8");
    private static final BigDecimal HIGH_ORDER_THRESHOLD = new BigDecimal("1000");
    private static final BigDecimal HIGH_ORDER_DISCOUNT_RATE = new BigDecimal("0.98");
    private static final int SCALE = 2;

    public record LineItem(BigDecimal price, int quantity) {
    }

    public BigDecimal calculateOrderTotal(List<LineItem> items, String customerType, String couponCode) {
        BigDecimal total = BigDecimal.ZERO;

        for (LineItem item : items) {
            BigDecimal linePrice = item.price().multiply(BigDecimal.valueOf(item.quantity()));
            if (item.quantity() > BULK_DISCOUNT_THRESHOLD) {
                linePrice = linePrice.multiply(BULK_DISCOUNT_RATE);
            }
            total = total.add(linePrice);
        }

        if ("vip".equals(customerType)) {
            total = total.multiply(VIP_DISCOUNT_RATE);
        } else if ("wholesale".equals(customerType)) {
            total = total.multiply(WHOLESALE_DISCOUNT_RATE);
        }

        if ("SAVE10".equals(couponCode)) {
            total = total.subtract(SAVE10_COUPON_VALUE);
        } else if ("SAVE20PERCENT".equals(couponCode)) {
            total = total.multiply(SAVE20_DISCOUNT_RATE);
        }

        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        if (total.compareTo(HIGH_ORDER_THRESHOLD) > 0) {
            total = total.multiply(HIGH_ORDER_DISCOUNT_RATE);
        }

        return total.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
