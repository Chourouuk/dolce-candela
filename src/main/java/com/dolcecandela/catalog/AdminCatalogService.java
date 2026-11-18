package com.dolcecandela.catalog;

import com.dolcecandela.catalog.dto.CatalogDtos.CategoryRequest;
import com.dolcecandela.catalog.dto.CatalogDtos.CategoryResponse;
import com.dolcecandela.catalog.dto.CatalogDtos.ImageResponse;
import com.dolcecandela.catalog.dto.CatalogDtos.OptionRequest;
import com.dolcecandela.catalog.dto.CatalogDtos.OptionResponse;
import com.dolcecandela.catalog.dto.CatalogDtos.ProductDetailResponse;
import com.dolcecandela.catalog.dto.CatalogDtos.ProductRequest;
import com.dolcecandela.common.ApiException;
import com.dolcecandela.common.Money;
import com.dolcecandela.storage.FileStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Gestion du catalogue par l'administrateur. */
@Service
@Transactional
public class AdminCatalogService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CustomizationOptionRepository optionRepository;
    private final FileStorageService storageService;

    public AdminCatalogService(CategoryRepository categoryRepository, ProductRepository productRepository,
                               CustomizationOptionRepository optionRepository, FileStorageService storageService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.optionRepository = optionRepository;
        this.storageService = storageService;
    }

    // ---------------- Produits ----------------

    @Transactional(readOnly = true)
    public Page<ProductDetailResponse> listAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(p -> CatalogMapper.toDetail(p, null, 0));
    }

    public ProductDetailResponse createProduct(ProductRequest request) {
        if (productRepository.existsBySlug(request.slug())) {
            throw ApiException.conflict("Ce slug est déjà utilisé par un autre produit.");
        }
        Product product = new Product();
        apply(product, request);
        productRepository.save(product);
        return CatalogMapper.toDetail(product, null, 0);
    }

    public ProductDetailResponse updateProduct(Long id, ProductRequest request) {
        Product product = findProduct(id);
        if (productRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw ApiException.conflict("Ce slug est déjà utilisé par un autre produit.");
        }
        apply(product, request);
        return CatalogMapper.toDetail(product, null, 0);
    }

    /** Suppression "douce" : le produit disparaît du site mais reste lié aux anciennes commandes. */
    public void deactivateProduct(Long id) {
        findProduct(id).setActive(false);
    }

    public ProductDetailResponse updateStock(Long id, int stockQuantity) {
        Product product = findProduct(id);
        product.setStockQuantity(stockQuantity);
        return CatalogMapper.toDetail(product, null, 0);
    }

    public ImageResponse addImage(Long productId, MultipartFile file, String altText, boolean main) {
        Product product = findProduct(productId);
        String url = storageService.store(file, FileStorageService.Folder.PRODUCTS);
        if (main) {
            product.getImages().forEach(image -> image.setMain(false));
        }
        ProductImage image = new ProductImage();
        image.setUrl(url);
        image.setAltText(altText == null || altText.isBlank() ? product.getNameFr() : altText.trim());
        image.setPosition(product.getImages().size());
        image.setMain(main || product.getImages().isEmpty());
        product.addImage(image);
        productRepository.flush();
        return CatalogMapper.toImageResponse(image);
    }

    public void deleteImage(Long productId, Long imageId) {
        Product product = findProduct(productId);
        ProductImage image = product.getImages().stream()
                .filter(i -> i.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Image introuvable."));
        product.getImages().remove(image);
        storageService.deleteQuietly(image.getUrl());
    }

    private void apply(Product product, ProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> ApiException.badRequest("Catégorie inconnue."));
        product.setCategory(category);
        product.setSlug(request.slug());
        product.setNameFr(request.nameFr().trim());
        product.setNameAr(request.nameAr().trim());
        product.setNameEn(request.nameEn());
        product.setDescriptionFr(request.descriptionFr());
        product.setDescriptionAr(request.descriptionAr());
        product.setDescriptionEn(request.descriptionEn());
        product.setPrice(Money.of(request.price()));
        product.setStockQuantity(request.stockQuantity());
        product.setMadeToOrder(request.madeToOrder());
        product.setCustomizable(request.customizable());
        product.setBadge(request.badge());
        product.setActive(request.active() == null || request.active());
        Set<Long> optionIds = request.optionIds() == null ? Set.of() : request.optionIds();
        List<CustomizationOption> options = optionRepository.findAllById(optionIds);
        if (options.size() != optionIds.size()) {
            throw ApiException.badRequest("Une ou plusieurs options de personnalisation sont inconnues.");
        }
        product.setCustomizationOptions(new HashSet<>(options));
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> ApiException.notFound("Produit introuvable."));
    }

    // ---------------- Catégories ----------------

    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsBySlug(request.slug())) {
            throw ApiException.conflict("Ce slug de catégorie existe déjà.");
        }
        Category category = new Category();
        apply(category, request);
        return CatalogMapper.toCategoryResponse(categoryRepository.save(category));
    }

    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Catégorie introuvable."));
        apply(category, request);
        return CatalogMapper.toCategoryResponse(category);
    }

    private static void apply(Category category, CategoryRequest request) {
        category.setSlug(request.slug());
        category.setNameFr(request.nameFr().trim());
        category.setNameAr(request.nameAr().trim());
        category.setNameEn(request.nameEn());
        category.setDescriptionFr(request.descriptionFr());
        category.setDescriptionAr(request.descriptionAr());
        category.setDescriptionEn(request.descriptionEn());
        category.setDisplayOrder(request.displayOrder());
        category.setActive(request.active() == null || request.active());
    }

    // ---------------- Options de personnalisation ----------------

    public OptionResponse createOption(OptionRequest request) {
        if (optionRepository.existsByTypeAndCode(request.type(), request.code())) {
            throw ApiException.conflict("Cette option existe déjà.");
        }
        CustomizationOption option = new CustomizationOption();
        apply(option, request);
        return CatalogMapper.toOptionResponse(optionRepository.save(option));
    }

    public OptionResponse updateOption(Long id, OptionRequest request) {
        CustomizationOption option = optionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Option introuvable."));
        apply(option, request);
        return CatalogMapper.toOptionResponse(option);
    }

    private static void apply(CustomizationOption option, OptionRequest request) {
        option.setType(request.type());
        option.setCode(request.code());
        option.setLabelFr(request.labelFr().trim());
        option.setLabelAr(request.labelAr().trim());
        option.setLabelEn(request.labelEn());
        option.setHexColor(request.hexColor());
        option.setExtraPrice(Money.of(request.extraPrice()));
        option.setActive(request.active() == null || request.active());
    }
}
