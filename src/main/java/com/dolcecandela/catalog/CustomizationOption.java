package com.dolcecandela.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Option de personnalisation : une couleur, un parfum, une taille ou un emballage. */
@Entity
@Table(name = "customization_options")
public class CustomizationOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", nullable = false, length = 20)
    private OptionType type;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "label_fr", nullable = false, length = 100)
    private String labelFr;

    @Column(name = "label_ar", nullable = false, length = 100)
    private String labelAr;

    @Column(name = "label_en", length = 100)
    private String labelEn;

    @Column(name = "hex_color", length = 7)
    private String hexColor;

    @Column(name = "extra_price", nullable = false, precision = 10, scale = 3)
    private BigDecimal extraPrice = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public OptionType getType() { return type; }
    public void setType(OptionType type) { this.type = type; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLabelFr() { return labelFr; }
    public void setLabelFr(String labelFr) { this.labelFr = labelFr; }
    public String getLabelAr() { return labelAr; }
    public void setLabelAr(String labelAr) { this.labelAr = labelAr; }
    public String getLabelEn() { return labelEn; }
    public void setLabelEn(String labelEn) { this.labelEn = labelEn; }
    public String getHexColor() { return hexColor; }
    public void setHexColor(String hexColor) { this.hexColor = hexColor; }
    public BigDecimal getExtraPrice() { return extraPrice; }
    public void setExtraPrice(BigDecimal extraPrice) { this.extraPrice = extraPrice; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
