package com.dolcecandela.order;

import com.dolcecandela.order.dto.OrderDtos.DeliveryUpdateRequest;
import com.dolcecandela.order.dto.OrderDtos.OrderResponse;
import com.dolcecandela.order.dto.OrderDtos.PaymentRequest;
import com.dolcecandela.order.dto.OrderDtos.QuoteRequest;
import com.dolcecandela.order.dto.OrderDtos.StatusUpdateRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Commandes")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public Page<OrderResponse> list(@RequestParam(required = false) OrderStatus status,
                                    @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return orderService.listForAdmin(status, pageable);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return orderService.getForAdmin(id);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return orderService.updateStatus(id, request.status());
    }

    @PatchMapping("/{id}/quote")
    public OrderResponse quote(@PathVariable Long id, @Valid @RequestBody QuoteRequest request) {
        return orderService.quoteCustomOrder(id, request.unitPrice());
    }

    @PatchMapping("/{id}/delivery")
    public OrderResponse updateDelivery(@PathVariable Long id, @Valid @RequestBody DeliveryUpdateRequest request) {
        return orderService.updateDelivery(id, request);
    }

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse addPayment(@PathVariable Long id, @Valid @RequestBody PaymentRequest request) {
        return orderService.addPayment(id, request);
    }
}
