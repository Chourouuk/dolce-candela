package com.dolcecandela.catalog;

import com.dolcecandela.catalog.dto.CatalogDtos.CategoryRequest;
import com.dolcecandela.catalog.dto.CatalogDtos.CategoryResponse;
import com.dolcecandela.catalog.dto.CatalogDtos.ImageResponse;
import com.dolcecandela.catalog.dto.CatalogDtos.OptionRequest;
import com.dolcecandela.catalog.dto.CatalogDtos.OptionResponse;
import com.dolcecandela.catalog.dto.CatalogDtos.ProductDetailResponse;
import com.dolcecandela.catalog.dto.CatalogDtos.ProductRequest;
import com.dolcecandela.catalog.dto.CatalogDtos.StockUpdateRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Espace admin : produits, stocks, images, catégories et options. Réservé au rôle ADMIN (cf. SecurityConfig). */
@Tag(name = "Admin - Catalogue")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin")
public class AdminCatalogController {

    private final AdminCatalogService service;

    public AdminCatalogController(AdminCatalogService service) {
        this.service = service;
    }

    @GetMapping("/products")
    public Page<ProductDetailResponse> listProducts(@PageableDefault(size = 20) Pageable pageable) {
        return service.listAllProducts(pageable);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDetailResponse createProduct(@Valid @RequestBody ProductRequest request) {
        return service.createProduct(request);
    }

    @PutMapping("/products/{id}")
    public ProductDetailResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return service.updateProduct(id, request);
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateProduct(@PathVariable Long id) {
        service.deactivateProduct(id);
    }

    @PatchMapping("/products/{id}/stock")
    public ProductDetailResponse updateStock(@PathVariable Long id, @Valid @RequestBody StockUpdateRequest request) {
        return service.updateStock(id, request.stockQuantity());
    }

    @PostMapping(value = "/products/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImageResponse addImage(@PathVariable Long id,
                                  @RequestParam("file") MultipartFile file,
                                  @RequestParam(required = false) String altText,
                                  @RequestParam(defaultValue = "false") boolean main) {
        return service.addImage(id, file, altText, main);
    }

    @DeleteMapping("/products/{id}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
        service.deleteImage(id, imageId);
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
        return service.createCategory(request);
    }

    @PutMapping("/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return service.updateCategory(id, request);
    }

    @PostMapping("/customization-options")
    @ResponseStatus(HttpStatus.CREATED)
    public OptionResponse createOption(@Valid @RequestBody OptionRequest request) {
        return service.createOption(request);
    }

    @PutMapping("/customization-options/{id}")
    public OptionResponse updateOption(@PathVariable Long id, @Valid @RequestBody OptionRequest request) {
        return service.updateOption(id, request);
    }
}
