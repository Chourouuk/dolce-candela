package com.dolcecandela.catalog;

import com.dolcecandela.catalog.dto.CatalogDtos.CategoryResponse;
import com.dolcecandela.catalog.dto.CatalogDtos.OptionResponse;
import com.dolcecandela.catalog.dto.CatalogDtos.ProductDetailResponse;
import com.dolcecandela.catalog.dto.CatalogDtos.ProductSummaryResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Catalogue (public)")
@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return catalogService.listCategories();
    }

    /**
     * Liste paginée. Exemples :
     * /api/products?category=bouquets-bougies&sort=price,asc
     * /api/products?badge=BESTSELLER&size=3
     * /api/products?q=rose
     */
    @GetMapping("/products")
    public Page<ProductSummaryResponse> products(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Badge badge,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean inStock,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return catalogService.searchProducts(category, badge, q, inStock, pageable);
    }

    @GetMapping("/products/{slug}")
    public ProductDetailResponse product(@PathVariable String slug) {
        return catalogService.getProduct(slug);
    }

    @GetMapping("/customization-options")
    public List<OptionResponse> options(@RequestParam(required = false) OptionType type) {
        return catalogService.listOptions(type);
    }
}
