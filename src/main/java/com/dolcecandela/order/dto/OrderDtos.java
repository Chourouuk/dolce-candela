package com.dolcecandela.order.dto;

import com.dolcecandela.catalog.OptionType;
import com.dolcecandela.delivery.DeliveryMethod;
import com.dolcecandela.delivery.DeliveryStatus;
import com.dolcecandela.order.Occasion;
import com.dolcecandela.order.OrderStatus;
import com.dolcecandela.order.OrderType;
import com.dolcecandela.payment.PaymentEnums.PaymentMethod;
import com.dolcecandela.payment.PaymentEnums.PaymentStatus;
import com.dolcecandela.payment.PaymentEnums.PaymentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    static final String PHONE_REGEX = "^\\+?[0-9 ]{8,20}$";

    // ======================= Requêtes client =======================

    public record DeliveryRequest(
            @NotNull DeliveryMethod method,
            @Size(max = 255) String addressLine,
            @Size(max = 100) String city,
            @Size(max = 100) String governorate,
            @Size(max = 10) String postalCode) {
    }

    public record OrderItemRequest(
            @NotNull Long productId,
            @NotNull @Min(1) @Max(100) Integer quantity,
            @Size(max = 10) List<@NotNull Long> optionIds,
            @Size(max = 200) String customText) {
    }

    /** Commande depuis le panier. */
    public record CreateOrderRequest(
            @NotBlank @Size(max = 120) String customerName,
            @NotBlank @Pattern(regexp = PHONE_REGEX, message = "Numéro de téléphone invalide") String customerPhone,
            @Email @Size(max = 160) String customerEmail,
            @NotEmpty @Size(max = 20) List<@Valid @NotNull OrderItemRequest> items,
            @NotNull @Valid DeliveryRequest delivery,
            @Size(max = 2000) String notes) {
    }

    /** Commande sur mesure (formulaire "Custom Order"). */
    public record CustomOrderRequest(
            @NotBlank @Size(max = 120) String customerName,
            @NotBlank @Pattern(regexp = PHONE_REGEX, message = "Numéro de téléphone invalide") String customerPhone,
            @Email @Size(max = 160) String customerEmail,
            @NotNull Occasion occasion,
            @Size(max = 5) List<@NotBlank String> colorCodes,
            @Size(max = 50) String scentCode,
            @Size(max = 200) String customText,
            @NotNull @Min(1) @Max(100) Integer quantity,
            @DecimalMin("0.000") @Digits(integer = 7, fraction = 3) BigDecimal budget,
            @Size(max = 2000) String notes,
            @Size(max = 500) String referenceImageUrl,
            @NotNull @Valid DeliveryRequest delivery) {
    }

    // ======================= Requêtes admin =======================

    public record StatusUpdateRequest(@NotNull OrderStatus status) {
    }

    public record QuoteRequest(
            @NotNull @DecimalMin(value = "0.001") @Digits(integer = 7, fraction = 3) BigDecimal unitPrice) {
    }

    public record PaymentRequest(
            @NotNull PaymentType type,
            @NotNull PaymentMethod method,
            @NotNull @DecimalMin(value = "0.001") @Digits(integer = 7, fraction = 3) BigDecimal amount,
            @Size(max = 100) String transactionRef) {
    }

    public record DeliveryUpdateRequest(LocalDate scheduledDate, @Size(max = 100) String trackingNumber) {
    }

    // ======================= Réponses =======================

    public record CustomizationResponse(OptionType type, String code, String label, BigDecimal extraPrice) {
    }

    public record OrderItemResponse(Long id, Long productId, String productName, BigDecimal unitPrice,
                                    int quantity, BigDecimal lineTotal, String customText,
                                    List<CustomizationResponse> customizations) {
    }

    public record DeliveryResponse(DeliveryMethod method, DeliveryStatus status, String addressLine, String city,
                                   String governorate, String postalCode, String pickupLocation, BigDecimal fee,
                                   LocalDate scheduledDate, String trackingNumber, Instant deliveredAt) {
    }

    public record PaymentResponse(Long id, PaymentType type, PaymentMethod method, BigDecimal amount,
                                  PaymentStatus status, String transactionRef, Instant paidAt) {
    }

    public record OrderResponse(Long id, String reference, OrderType type, OrderStatus status,
                                String customerName, String customerPhone, String customerEmail,
                                Occasion occasion, BigDecimal budget, String referenceImageUrl, String notes,
                                boolean quoted, List<OrderItemResponse> items, DeliveryResponse delivery,
                                BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal total,
                                BigDecimal depositAmount, BigDecimal amountPaid, BigDecimal balanceDue,
                                List<PaymentResponse> payments, Instant createdAt, Instant updatedAt) {
    }

    /** Suivi public : aucune donnée personnelle. */
    public record OrderTrackingResponse(String reference, OrderType type, OrderStatus status,
                                        DeliveryMethod deliveryMethod, DeliveryStatus deliveryStatus,
                                        boolean quoted, BigDecimal total, BigDecimal depositAmount,
                                        BigDecimal amountPaid, Instant createdAt, Instant updatedAt) {
    }
}
