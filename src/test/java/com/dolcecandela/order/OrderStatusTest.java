package com.dolcecandela.order;

import org.junit.jupiter.api.Test;

import static com.dolcecandela.order.OrderStatus.CANCELLED;
import static com.dolcecandela.order.OrderStatus.CONFIRMED;
import static com.dolcecandela.order.OrderStatus.DELIVERED;
import static com.dolcecandela.order.OrderStatus.IN_PREPARATION;
import static com.dolcecandela.order.OrderStatus.PENDING;
import static com.dolcecandela.order.OrderStatus.READY_FOR_PICKUP;
import static com.dolcecandela.order.OrderStatus.SHIPPED;
import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void happyPathIsAllowed() {
        assertThat(PENDING.canTransitionTo(CONFIRMED)).isTrue();
        assertThat(CONFIRMED.canTransitionTo(IN_PREPARATION)).isTrue();
        assertThat(IN_PREPARATION.canTransitionTo(SHIPPED)).isTrue();
        assertThat(IN_PREPARATION.canTransitionTo(READY_FOR_PICKUP)).isTrue();
        assertThat(SHIPPED.canTransitionTo(DELIVERED)).isTrue();
        assertThat(READY_FOR_PICKUP.canTransitionTo(DELIVERED)).isTrue();
    }

    @Test
    void skippingStepsOrLeavingFinalStatesIsForbidden() {
        assertThat(PENDING.canTransitionTo(SHIPPED)).isFalse();
        assertThat(PENDING.canTransitionTo(DELIVERED)).isFalse();
        assertThat(SHIPPED.canTransitionTo(CANCELLED)).isFalse();
        assertThat(DELIVERED.canTransitionTo(PENDING)).isFalse();
        assertThat(CANCELLED.canTransitionTo(CONFIRMED)).isFalse();
    }
}
