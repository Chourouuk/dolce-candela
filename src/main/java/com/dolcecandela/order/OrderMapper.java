package com.dolcecandela.order;

import com.dolcecandela.delivery.Delivery;
import com.dolcecandela.order.dto.OrderDtos.CustomizationResponse;
import com.dolcecandela.order.dto.OrderDtos.DeliveryResponse;
import com.dolcecandela.order.dto.OrderDtos.OrderItemResponse;
import com.dolcecandela.order.dto.OrderDtos.OrderResponse;
import com.dolcecandela.order.dto.OrderDtos.OrderTrackingResponse;
import com.dolcecandela.order.dto.OrderDtos.PaymentResponse;
import com.dolcecandela.payment.Payment;

public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponse toResponse(Order o) {
        return new OrderResponse(o.getId(), o.getReference(), o.getType(), o.getStatus(),
                o.getCustomerName(), o.getCustomerPhone(), o.getCustomerEmail(),
                o.getOccasion(), o.getBudget(), o.getReferenceImageUrl(), o.getNotes(), o.isQuoted(),
                o.getItems().stream().map(OrderMapper::toItemResponse).toList(),
                toDeliveryResponse(o.getDelivery()),
                o.getSubtotal(), o.getDeliveryFee(), o.getTotal(), o.getDepositAmount(),
                o.amountPaid(), o.balanceDue(),
                o.getPayments().stream().map(OrderMapper::toPaymentResponse).toList(),
                o.getCreatedAt(), o.getUpdatedAt());
    }

    public static OrderTrackingResponse toTracking(Order o) {
        Delivery d = o.getDelivery();
        return new OrderTrackingResponse(o.getReference(), o.getType(), o.getStatus(),
                d == null ? null : d.getMethod(), d == null ? null : d.getStatus(),
                o.isQuoted(), o.getTotal(), o.getDepositAmount(), o.amountPaid(),
                o.getCreatedAt(), o.getUpdatedAt());
    }

    private static OrderItemResponse toItemResponse(OrderItem i) {
        return new OrderItemResponse(i.getId(), i.getProduct() == null ? null : i.getProduct().getId(),
                i.getProductName(), i.getUnitPrice(), i.getQuantity(), i.getLineTotal(), i.getCustomText(),
                i.getCustomizations().stream()
                        .map(c -> new CustomizationResponse(c.getOptionType(), c.getOptionCode(), c.getLabel(), c.getExtraPrice()))
                        .toList());
    }

    private static DeliveryResponse toDeliveryResponse(Delivery d) {
        if (d == null) {
            return null;
        }
        return new DeliveryResponse(d.getMethod(), d.getStatus(), d.getAddressLine(), d.getCity(),
                d.getGovernorate(), d.getPostalCode(), d.getPickupLocation(), d.getFee(),
                d.getScheduledDate(), d.getTrackingNumber(), d.getDeliveredAt());
    }

    private static PaymentResponse toPaymentResponse(Payment p) {
        return new PaymentResponse(p.getId(), p.getType(), p.getMethod(), p.getAmount(), p.getStatus(),
                p.getTransactionRef(), p.getPaidAt());
    }
}
