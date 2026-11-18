package com.dolcecandela.admin;

import com.dolcecandela.catalog.ProductRepository;
import com.dolcecandela.order.OrderRepository;
import com.dolcecandela.order.OrderService;
import com.dolcecandela.order.OrderStatus;
import com.dolcecandela.payment.PaymentEnums.PaymentStatus;
import com.dolcecandela.review.ReviewRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Chiffres clés pour la page d'accueil de l'espace admin. */
@Tag(name = "Admin - Tableau de bord")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    public record LowStockItem(Long id, String slug, String nameFr, int stockQuantity) {
    }

    public record DashboardResponse(Map<OrderStatus, Long> ordersByStatus,
                                    BigDecimal deliveredRevenue,
                                    BigDecimal paymentsCollected,
                                    long pendingReviews,
                                    List<LowStockItem> lowStock) {
    }

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;

    public AdminDashboardController(OrderService orderService, OrderRepository orderRepository,
                                    ProductRepository productRepository, ReviewRepository reviewRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public DashboardResponse dashboard(@RequestParam(defaultValue = "3") int lowStockThreshold) {
        List<LowStockItem> lowStock = productRepository
                .findAllByActiveTrueAndMadeToOrderFalseAndStockQuantityLessThanEqualOrderByStockQuantityAsc(lowStockThreshold)
                .stream()
                .map(p -> new LowStockItem(p.getId(), p.getSlug(), p.getNameFr(), p.getStockQuantity()))
                .toList();
        return new DashboardResponse(
                orderService.countByStatus(),
                orderRepository.sumTotalByStatus(OrderStatus.DELIVERED),
                orderRepository.sumPaymentsByStatus(PaymentStatus.CONFIRMED),
                reviewRepository.countByApprovedFalse(),
                lowStock);
    }
}
