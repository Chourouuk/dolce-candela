package com.dolcecandela.catalog.dto;

import com.dolcecandela.catalog.Badge;
import com.dolcecandela.catalog.OptionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * DTOs du catalogue. Les textes sont renvoyés dans les 3 langues (fr / ar / en) :
 * le front choisit selon la langue active (et passe en RTL pour l'arabe).
 */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    private static final String SLUG_REGEX = "^[a-z0-9]+(-[a-z0-9]+)*$";

    // ---------------- Réponses ----------------

    public record CategoryResponse(Long id, String slug, String nameFr, String nameAr, String nameEn,
                                   String descriptionFr, String descriptionAr, String descriptionEn,
                                   int displayOrder, boolean active) {
    }

    public record ImageResponse(Long id, String url, String altText, int position, boolean main) {
    }

    public record OptionResponse(Long id, OptionType type, String code, String labelFr, String labelAr,
                                 String labelEn, String hexColor, BigDecimal extraPrice) {
    }

    public record ProductSummaryResponse(Long id, String slug, String categorySlug,
                                         String nameFr, String nameAr, String nameEn,
                                         BigDecimal price, Badge badge, String mainImageUrl,
                                         boolean inStock, boolean madeToOrder, boolean customizable) {
    }

    public record ProductDetailResponse(Long id, String slug, CategoryResponse category,
                                        String nameFr, String nameAr, String nameEn,
                                        String descriptionFr, String descriptionAr, String descriptionEn,
                                        BigDecimal price, int stockQuantity, boolean inStock,
                                        boolean madeToOrder, boolean customizable, Badge badge, boolean active,
                                        List<ImageResponse> images, List<OptionResponse> options,
                                        Double averageRating, long reviewCount) {
    }

    // ---------------- Requêtes admin ----------------

    public record CategoryRequest(
            @NotBlank @Size(max = 80) @Pattern(regexp = SLUG_REGEX, message = "Slug invalide (ex. bougies-decoratives)") String slug,
            @NotBlank @Size(max = 120) String nameFr,
            @NotBlank @Size(max = 120) String nameAr,
            @Size(max = 120) String nameEn,
            @Size(max = 1000) String descriptionFr,
            @Size(max = 1000) String descriptionAr,
            @Size(max = 1000) String descriptionEn,
            @Min(0) int displayOrder,
            Boolean active) {
    }

    public record ProductRequest(
            @NotNull Long categoryId,
            @NotBlank @Size(max = 120) @Pattern(regexp = SLUG_REGEX, message = "Slug invalide (ex. bouquet-tulipes)") String slug,
            @NotBlank @Size(max = 150) String nameFr,
            @NotBlank @Size(max = 150) String nameAr,
            @Size(max = 150) String nameEn,
            @Size(max = 2000) String descriptionFr,
            @Size(max = 2000) String descriptionAr,
            @Size(max = 2000) String descriptionEn,
            @NotNull @DecimalMin("0.000") @Digits(integer = 7, fraction = 3) BigDecimal price,
            @Min(0) int stockQuantity,
            boolean madeToOrder,
            boolean customizable,
            Badge badge,
            Boolean active,
            Set<Long> optionIds) {
    }

    public record StockUpdateRequest(@NotNull @Min(0) Integer stockQuantity) {
    }

    public record OptionRequest(
            @NotNull OptionType type,
            @NotBlank @Size(max = 50) @Pattern(regexp = SLUG_REGEX, message = "Code invalide (ex. fleur-oranger)") String code,
            @NotBlank @Size(max = 100) String labelFr,
            @NotBlank @Size(max = 100) String labelAr,
            @Size(max = 100) String labelEn,
            @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Couleur au format #RRGGBB") String hexColor,
            @NotNull @DecimalMin("0.000") @Digits(integer = 7, fraction = 3) BigDecimal extraPrice,
            Boolean active) {
    }
}
