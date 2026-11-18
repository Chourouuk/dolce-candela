package com.dolcecandela.catalog;

import com.dolcecandela.catalog.dto.CatalogDtos.CategoryResponse;
import com.dolcecandela.catalog.dto.CatalogDtos.OptionResponse;
import com.dolcecandela.catalog.dto.CatalogDtos.ProductDetailResponse;
import com.dolcecandela.catalog.dto.CatalogDtos.ProductSummaryResponse;
import com.dolcecandela.common.ApiException;
import com.dolcecandela.review.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** Lecture publique du catalogue. */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CustomizationOptionRepository optionRepository;
    private final ReviewRepository reviewRepository;

    public CatalogService(CategoryRepository categoryRepository, ProductRepository productRepository,
                          CustomizationOptionRepository optionRepository, ReviewRepository reviewRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.optionRepository = optionRepository;
        this.reviewRepository = reviewRepository;
    }

    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(CatalogMapper::toCategoryResponse)
                .toList();
    }

    public Page<ProductSummaryResponse> searchProducts(String categorySlug, Badge badge, String query,
                                                       boolean inStockOnly, Pageable pageable) {
        Specification<Product> spec = (root, q, cb) -> cb.isTrue(root.get("active"));
        if (categorySlug != null && !categorySlug.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("category").get("slug"), categorySlug));
        }
        if (badge != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("badge"), badge));
        }
        if (query != null && !query.isBlank()) {
            String pattern = "%" + escapeLike(query.trim().toLowerCase(Locale.ROOT)) + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("nameFr")), pattern, '\\'),
                    cb.like(cb.lower(root.get("nameAr")), pattern, '\\'),
                    cb.like(cb.lower(root.get("nameEn")), pattern, '\\')));
        }
        if (inStockOnly) {
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.isTrue(root.get("madeToOrder")),
                    cb.greaterThan(root.get("stockQuantity"), 0)));
        }
        return productRepository.findAll(spec, pageable).map(CatalogMapper::toSummary);
    }

    public ProductDetailResponse getProduct(String slug) {
        Product product = productRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> ApiException.notFound("Produit introuvable."));
        Double average = reviewRepository.averageRating(product.getId());
        long count = reviewRepository.countByProduct_IdAndApprovedTrue(product.getId());
        return CatalogMapper.toDetail(product, average == null ? null : Math.round(average * 10) / 10.0, count);
    }

    public List<OptionResponse> listOptions(OptionType type) {
        List<CustomizationOption> options = type == null
                ? optionRepository.findAllByActiveTrueOrderByTypeAscIdAsc()
                : optionRepository.findAllByTypeAndActiveTrueOrderByIdAsc(type);
        return options.stream().map(CatalogMapper::toOptionResponse).toList();
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
