package com.dolcecandela.order;

import com.dolcecandela.catalog.CustomizationOption;
import com.dolcecandela.catalog.OptionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Option choisie pour une ligne de commande (copie figée : un changement de prix ultérieur n'impacte pas la commande). */
@Entity
@Table(name = "order_item_customizations")
public class OrderItemCustomization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id")
    private CustomizationOption option;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", nullable = false, length = 20)
    private OptionType optionType;

    @Column(name = "option_code", nullable = false, length = 50)
    private String optionCode;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(name = "extra_price", nullable = false, precision = 10, scale = 3)
    private BigDecimal extraPrice = BigDecimal.ZERO;

    public static OrderItemCustomization from(CustomizationOption option) {
        OrderItemCustomization c = new OrderItemCustomization();
        c.option = option;
        c.optionType = option.getType();
        c.optionCode = option.getCode();
        c.label = option.getLabelFr();
        c.extraPrice = option.getExtraPrice();
        return c;
    }

    public Long getId() { return id; }
    public OrderItem getItem() { return item; }
    public void setItem(OrderItem item) { this.item = item; }
    public CustomizationOption getOption() { return option; }
    public OptionType getOptionType() { return optionType; }
    public String getOptionCode() { return optionCode; }
    public String getLabel() { return label; }
    public BigDecimal getExtraPrice() { return extraPrice; }
}
