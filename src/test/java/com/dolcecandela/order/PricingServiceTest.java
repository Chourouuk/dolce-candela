package com.dolcecandela.order;

import com.dolcecandela.catalog.CustomizationOption;
import com.dolcecandela.config.AppProperties;
import com.dolcecandela.delivery.Delivery;
import com.dolcecandela.delivery.DeliveryMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {

    private final PricingService pricing = new PricingService(new AppProperties(
            new AppProperties.Jwt("x".repeat(32), 60),
            new AppProperties.Cors(List.of()),
            new AppProperties.Admin(null, null, null),
            new AppProperties.Business(new BigDecimal("8.000"), 30, 10, "Monastir"),
            new AppProperties.Storage("target/x"),
            new AppProperties.Security(5, 15)));

    private static CustomizationOption option(String extra) {
        CustomizationOption o = new CustomizationOption();
        o.setExtraPrice(new BigDecimal(extra));
        return o;
    }

    @Test
    void unitPriceAddsOptionExtras() {
        assertThat(pricing.unitPrice(new BigDecimal("65"), List.of(option("10.000"), option("5.000"))))
                .isEqualByComparingTo("80.000");
    }

    @Test
    void deliveryCostsEightDinarsAndPickupIsFree() {
        assertThat(pricing.deliveryFee(DeliveryMethod.DELIVERY)).isEqualByComparingTo("8.000");
        assertThat(pricing.deliveryFee(DeliveryMethod.PICKUP)).isEqualByComparingTo("0");
    }

    @Test
    void standardOrderHasNoDeposit() {
        Order order = orderWith(DeliveryMethod.DELIVERY, "42.000", 2, false);
        pricing.recalculate(order);
        assertThat(order.getSubtotal()).isEqualByComparingTo("84.000");
        assertThat(order.getDeliveryFee()).isEqualByComparingTo("8.000");
        assertThat(order.getTotal()).isEqualByComparingTo("92.000");
        assertThat(order.getDepositAmount()).isEqualByComparingTo("0");
    }

    @Test
    void smallPersonalizedOrderHasNoDeposit() {
        Order order = orderWith(DeliveryMethod.PICKUP, "89.000", 1, true);
        pricing.recalculate(order);
        assertThat(order.getTotal()).isEqualByComparingTo("89.000");
        assertThat(order.getDepositAmount()).isEqualByComparingTo("0");
    }

    @Test
    void largeQuantityRequiresThirtyPercentDeposit() {
        Order order = orderWith(DeliveryMethod.PICKUP, "89.000", 10, false);
        pricing.recalculate(order);
        assertThat(order.getTotal()).isEqualByComparingTo("890.000");
        assertThat(order.getDepositAmount()).isEqualByComparingTo("267.000");
    }

    @Test
    void depositThresholdCountsAllLines() {
        Order order = orderWith(DeliveryMethod.PICKUP, "42.000", 6, false);
        OrderItem second = new OrderItem();
        second.setProductName("Test 2");
        second.setUnitPrice(new BigDecimal("39.000"));
        second.setQuantity(4);
        order.addItem(second);
        pricing.recalculate(order);
        assertThat(order.getTotal()).isEqualByComparingTo("408.000");
        assertThat(order.getDepositAmount()).isEqualByComparingTo("122.400");
    }

    @Test
    void customOrderWithoutQuoteHasNoDepositYet() {
        Order order = orderWith(DeliveryMethod.DELIVERY, "0", 20, true);
        order.setType(OrderType.CUSTOM);
        order.setQuoted(false);
        pricing.recalculate(order);
        assertThat(order.getTotal()).isEqualByComparingTo("8.000");
        assertThat(order.getDepositAmount()).isEqualByComparingTo("0");
    }

    private static Order orderWith(DeliveryMethod method, String unitPrice, int quantity, boolean personalized) {
        Order order = new Order();
        OrderItem item = new OrderItem();
        item.setProductName("Test");
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setQuantity(quantity);
        if (personalized) {
            item.setCustomText("Emma & Tom");
        }
        order.addItem(item);
        Delivery delivery = new Delivery();
        delivery.setMethod(method);
        order.attachDelivery(delivery);
        return order;
    }
}
