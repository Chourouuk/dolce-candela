package com.dolcecandela.review;

import com.dolcecandela.catalog.Product;
import com.dolcecandela.catalog.ProductRepository;
import com.dolcecandela.common.ApiException;
import com.dolcecandela.order.OrderRepository;
import com.dolcecandela.order.OrderStatus;
import com.dolcecandela.security.AuthenticatedUser;
import com.dolcecandela.user.UserRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class ReviewService {

    public record ReviewRequest(@NotNull @Min(1) @Max(5) Integer rating, @Size(max = 1000) String comment) {
    }

    public record ReviewResponse(Long id, String productSlug, int rating, String comment, String authorName,
                                 boolean approved, Instant createdAt) {
    }

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository,
                         OrderRepository orderRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> approvedReviews(String productSlug, Pageable pageable) {
        Product product = findProduct(productSlug);
        return reviewRepository.findAllByProduct_IdAndApprovedTrue(product.getId(), pageable).map(ReviewService::toResponse);
    }

    /** Seul un client ayant reçu le produit peut laisser un avis (évite les faux avis). */
    public ReviewResponse create(String productSlug, AuthenticatedUser user, ReviewRequest request) {
        Product product = findProduct(productSlug);
        if (orderRepository.countPurchases(user.id(), product.getId(), OrderStatus.DELIVERED) == 0) {
            throw ApiException.forbidden("Vous pourrez laisser un avis après avoir reçu ce produit.");
        }
        if (reviewRepository.existsByProduct_IdAndUser_Id(product.getId(), user.id())) {
            throw ApiException.conflict("Vous avez déjà donné votre avis sur ce produit.");
        }
        Review review = new Review();
        review.setProduct(product);
        review.setUser(userRepository.getReferenceById(user.id()));
        review.setRating(request.rating());
        review.setComment(request.comment() == null || request.comment().isBlank() ? null : request.comment().trim());
        review.setApproved(false);
        return toResponse(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> pendingReviews(Pageable pageable) {
        return reviewRepository.findAllByApprovedFalse(pageable).map(ReviewService::toResponse);
    }

    public ReviewResponse approve(Long id) {
        Review review = reviewRepository.findById(id).orElseThrow(() -> ApiException.notFound("Avis introuvable."));
        review.setApproved(true);
        return toResponse(review);
    }

    public void delete(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw ApiException.notFound("Avis introuvable.");
        }
        reviewRepository.deleteById(id);
    }

    private Product findProduct(String slug) {
        return productRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> ApiException.notFound("Produit introuvable."));
    }

    private static ReviewResponse toResponse(Review r) {
        return new ReviewResponse(r.getId(), r.getProduct().getSlug(), r.getRating(), r.getComment(),
                shortName(r.getUser().getFullName()), r.isApproved(), r.getCreatedAt());
    }

    /** "Chourouk Chedly" → "Chourouk C." : on n'affiche pas le nom complet publiquement. */
    static String shortName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Client";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts.length == 1 ? parts[0] : parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }
}
