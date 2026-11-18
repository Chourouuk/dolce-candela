package com.dolcecandela.catalog;

import com.dolcecandela.catalog.dto.CatalogDtos.CategoryResponse;
import com.dolcecandela.catalog.dto.CatalogDtos.ImageResponse;
import com.dolcecandela.catalog.dto.CatalogDtos.OptionResponse;
import com.dolcecandela.catalog.dto.CatalogDtos.ProductDetailResponse;
import com.dolcecandela.catalog.dto.CatalogDtos.ProductSummaryResponse;

import java.util.Comparator;
import java.util.List;

/** Conversion entités → DTO (à appeler dans une transaction : les relations sont chargées à la demande). */
public final class CatalogMapper {

    private CatalogMapper() {
    }

    public static CategoryResponse toCategoryResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getSlug(), c.getNameFr(), c.getNameAr(), c.getNameEn(),
                c.getDescriptionFr(), c.getDescriptionAr(), c.getDescriptionEn(), c.getDisplayOrder(), c.isActive());
    }

    public static ImageResponse toImageResponse(ProductImage i) {
        return new ImageResponse(i.getId(), i.getUrl(), i.getAltText(), i.getPosition(), i.isMain());
    }

    public static OptionResponse toOptionResponse(CustomizationOption o) {
        return new OptionResponse(o.getId(), o.getType(), o.getCode(), o.getLabelFr(), o.getLabelAr(),
                o.getLabelEn(), o.getHexColor(), o.getExtraPrice());
    }

    public static ProductSummaryResponse toSummary(Product p) {
        String mainImage = p.mainImage().map(ProductImage::getUrl).orElse(null);
        return new ProductSummaryResponse(p.getId(), p.getSlug(), p.getCategory().getSlug(),
                p.getNameFr(), p.getNameAr(), p.getNameEn(), p.getPrice(), p.getBadge(), mainImage,
                p.isInStock(), p.isMadeToOrder(), p.isCustomizable());
    }

    public static ProductDetailResponse toDetail(Product p, Double averageRating, long reviewCount) {
        List<ImageResponse> images = p.getImages().stream().map(CatalogMapper::toImageResponse).toList();
        List<OptionResponse> options = p.getCustomizationOptions().stream()
                .filter(CustomizationOption::isActive)
                .sorted(Comparator.comparing(CustomizationOption::getType).thenComparing(CustomizationOption::getId))
                .map(CatalogMapper::toOptionResponse)
                .toList();
        return new ProductDetailResponse(p.getId(), p.getSlug(), toCategoryResponse(p.getCategory()),
                p.getNameFr(), p.getNameAr(), p.getNameEn(),
                p.getDescriptionFr(), p.getDescriptionAr(), p.getDescriptionEn(),
                p.getPrice(), p.getStockQuantity(), p.isInStock(), p.isMadeToOrder(), p.isCustomizable(),
                p.getBadge(), p.isActive(), images, options, averageRating, reviewCount);
    }
}
