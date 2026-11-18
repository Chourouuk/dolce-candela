package com.dolcecandela.order;

/**
 * Cycle de vie d'une commande :
 * PENDING → CONFIRMED → IN_PREPARATION → SHIPPED (livraison) ou READY_FOR_PICKUP (retrait) → DELIVERED
 * Annulation possible jusqu'à IN_PREPARATION inclus.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    IN_PREPARATION,
    READY_FOR_PICKUP,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == IN_PREPARATION || next == CANCELLED;
            case IN_PREPARATION -> next == SHIPPED || next == READY_FOR_PICKUP || next == CANCELLED;
            case SHIPPED, READY_FOR_PICKUP -> next == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
