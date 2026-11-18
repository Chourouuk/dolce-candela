package com.dolcecandela.payment;

/** Énumérations liées aux paiements. */
public final class PaymentEnums {

    private PaymentEnums() {
    }

    public enum PaymentType {
        /** Acompte pour confirmer une commande personnalisée. */
        DEPOSIT,
        /** Solde restant. */
        BALANCE,
        /** Paiement intégral en une fois. */
        FULL
    }

    public enum PaymentMethod {
        CASH,
        BANK_TRANSFER,
        D17,
        FLOUCI,
        CARD
    }

    public enum PaymentStatus {
        PENDING,
        CONFIRMED,
        REFUNDED
    }
}
