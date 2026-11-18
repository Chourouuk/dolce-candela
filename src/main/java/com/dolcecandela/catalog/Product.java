package com.dolcecandela.catalog;

import com.dolcecandela.common.ApiException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(name = "name_fr", nullable = false, length = 150)
    private String nameFr;

    @Column(name = "name_ar", nullable = false, length = 150)
    private String nameAr;

    @Column(name = "name_en", length = 150)
    private String nameEn;

    @Column(name = "description_fr", length = 2000)
    private String descriptionFr;

    @Column(name = "description_ar", length = 2000)
    private String descriptionAr;

    @Column(name = "description_en", length = 2000)
    private String descriptionEn;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    /** Fabriqué à la commande : pas de contrôle de stock. */
    @Column(name = "made_to_order", nullable = false)
    private boolean madeToOrder;

    @Column(nullable = false)
    private boolean customizable;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Badge badge;

    @Column(nullable = false)
    private boolean active = true;

    /** Verrouillage optimiste : deux commandes simultanées ne peuvent pas vendre la même dernière pièce. */
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<ProductImage> images = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "product_customization_options",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "option_id"))
    private Set<CustomizationOption> customizationOptions = new HashSet<>();

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // ---------- Règles métier ----------

    public boolean isInStock() {
        return madeToOrder || stockQuantity > 0;
    }

    public void decreaseStock(int quantity) {
        if (madeToOrder) {
            return;
        }
        if (stockQuantity < quantity) {
            throw ApiException.conflict("Stock insuffisant pour « " + nameFr + " » (disponible : " + stockQuantity + ").");
        }
        stockQuantity -= quantity;
    }

    public void increaseStock(int quantity) {
        if (!madeToOrder) {
            stockQuantity += quantity;
        }
    }

    public boolean allowsOption(CustomizationOption option) {
        return customizationOptions.stream().anyMatch(o -> o.getId().equals(option.getId()));
    }

    public Optional<ProductImage> mainImage() {
        return images.stream().filter(ProductImage::isMain).findFirst()
                .or(() -> images.stream().findFirst());
    }

    public void addImage(ProductImage image) {
        image.setProduct(this);
        images.add(image);
    }

    // ---------- Accesseurs ----------

    public Long getId() { return id; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getNameFr() { return nameFr; }
    public void setNameFr(String nameFr) { this.nameFr = nameFr; }
    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public String getDescriptionFr() { return descriptionFr; }
    public void setDescriptionFr(String descriptionFr) { this.descriptionFr = descriptionFr; }
    public String getDescriptionAr() { return descriptionAr; }
    public void setDescriptionAr(String descriptionAr) { this.descriptionAr = descriptionAr; }
    public String getDescriptionEn() { return descriptionEn; }
    public void setDescriptionEn(String descriptionEn) { this.descriptionEn = descriptionEn; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public boolean isMadeToOrder() { return madeToOrder; }
    public void setMadeToOrder(boolean madeToOrder) { this.madeToOrder = madeToOrder; }
    public boolean isCustomizable() { return customizable; }
    public void setCustomizable(boolean customizable) { this.customizable = customizable; }
    public Badge getBadge() { return badge; }
    public void setBadge(Badge badge) { this.badge = badge; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<ProductImage> getImages() { return images; }
    public Set<CustomizationOption> getCustomizationOptions() { return customizationOptions; }
    public void setCustomizationOptions(Set<CustomizationOption> options) { this.customizationOptions = options; }
}
