package com.dolcecandela.order;

import com.dolcecandela.catalog.CustomizationOption;
import com.dolcecandela.common.Money;
import com.dolcecandela.config.AppProperties;
import com.dolcecandela.delivery.DeliveryMethod;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * Tous les montants sont calculés côté serveur : on ne fait jamais confiance
 * à un prix envoyé par le navigateur.
 */
@Service
public class PricingService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BigDecimal deliveryFee;
    private final BigDecimal depositPercent;

    public PricingService(AppProperties properties) {
        this.deliveryFee = Money.of(properties.business().deliveryFee());
        this.depositPercent = BigDecimal.valueOf(properties.business().depositPercent());
    }

    /** Prix unitaire = prix du produit + suppléments des options choisies. */
    public BigDecimal unitPrice(BigDecimal basePrice, Collection<CustomizationOption> options) {
        BigDecimal extras = options.stream()
                .map(CustomizationOption::getExtraPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Money.of(basePrice.add(extras));
    }

    public BigDecimal lineTotal(BigDecimal unitPrice, int quantity) {
        return Money.of(unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    public BigDecimal deliveryFee(DeliveryMethod method) {
        return method == DeliveryMethod.DELIVERY ? deliveryFee : Money.zero();
    }

    public BigDecimal deposit(BigDecimal total) {
        return Money.of(total.multiply(depositPercent).divide(HUNDRED));
    }

    /** Recalcule sous-total, frais de livraison, total et acompte de la commande. */
    public void recalculate(Order order) {
        for (OrderItem item : order.getItems()) {
            item.setLineTotal(lineTotal(item.getUnitPrice(), item.getQuantity()));
        }
        BigDecimal subtotal = Money.of(order.getItems().stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal fee = order.getDelivery() == null ? Money.zero() : deliveryFee(order.getDelivery().getMethod());
        BigDecimal total = Money.of(subtotal.add(fee));

        order.setSubtotal(subtotal);
        order.setDeliveryFee(fee);
        if (order.getDelivery() != null) {
            order.getDelivery().setFee(fee);
        }
        order.setTotal(total);
        // Pas d'acompte tant que le prix d'une commande sur mesure n'est pas fixé
        order.setDepositAmount(order.isQuoted() && order.requiresDeposit() ? deposit(total) : Money.zero());
    }
}
