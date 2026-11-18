package com.dolcecandela.order;

public enum OrderType {
    /** Commande de produits du catalogue (via le panier). */
    STANDARD,
    /** Demande de création sur mesure (formulaire "Custom Order"), prix fixé ensuite par l'admin. */
    CUSTOM
}
