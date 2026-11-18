package com.dolcecandela.order;

import com.dolcecandela.order.dto.OrderDtos.CreateOrderRequest;
import com.dolcecandela.order.dto.OrderDtos.CustomOrderRequest;
import com.dolcecandela.order.dto.OrderDtos.OrderResponse;
import com.dolcecandela.order.dto.OrderDtos.OrderTrackingResponse;
import com.dolcecandela.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Commandes (client)")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** Valider le panier. Public : un invité peut commander (le compte est lié si un jeton est fourni). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request,
                                @AuthenticationPrincipal AuthenticatedUser user) {
        return orderService.createOrder(request, user);
    }

    /** Formulaire "Custom Order". */
    @PostMapping("/custom")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createCustom(@Valid @RequestBody CustomOrderRequest request,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        return orderService.createCustomOrder(request, user);
    }

    /** Suivi public : /api/orders/track?reference=DC-261113-ABC123&phone=+21612345678 */
    @GetMapping("/track")
    public OrderTrackingResponse track(@RequestParam String reference, @RequestParam String phone) {
        return orderService.track(reference, phone);
    }

    @GetMapping("/me")
    public Page<OrderResponse> myOrders(@AuthenticationPrincipal AuthenticatedUser user,
                                        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return orderService.myOrders(user, pageable);
    }

    @GetMapping("/me/{reference}")
    public OrderResponse myOrder(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String reference) {
        return orderService.myOrder(user, reference);
    }

    @PostMapping("/me/{reference}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String reference) {
        return orderService.cancelMyOrder(user, reference);
    }
}
