package com.dolcecandela.review;

import com.dolcecandela.review.ReviewService.ReviewRequest;
import com.dolcecandela.review.ReviewService.ReviewResponse;
import com.dolcecandela.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Avis clients")
@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/api/products/{slug}/reviews")
    public Page<ReviewResponse> list(@PathVariable String slug,
                                     @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return reviewService.approvedReviews(slug, pageable);
    }

    @PostMapping("/api/products/{slug}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@PathVariable String slug,
                                 @AuthenticationPrincipal AuthenticatedUser user,
                                 @Valid @RequestBody ReviewRequest request) {
        return reviewService.create(slug, user, request);
    }

    // ---------- Modération (admin) ----------

    @GetMapping("/api/admin/reviews/pending")
    public Page<ReviewResponse> pending(@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return reviewService.pendingReviews(pageable);
    }

    @PatchMapping("/api/admin/reviews/{id}/approve")
    public ReviewResponse approve(@PathVariable Long id) {
        return reviewService.approve(id);
    }

    @DeleteMapping("/api/admin/reviews/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        reviewService.delete(id);
    }
}
