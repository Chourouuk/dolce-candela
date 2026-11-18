package com.dolcecandela.order;

import com.dolcecandela.catalog.CustomizationOptionRepository;
import com.dolcecandela.catalog.Product;
import com.dolcecandela.catalog.ProductRepository;
import com.dolcecandela.common.ApiException;
import com.dolcecandela.config.AppProperties;
import com.dolcecandela.delivery.Delivery;
import com.dolcecandela.delivery.DeliveryMethod;
import com.dolcecandela.delivery.DeliveryStatus;
import com.dolcecandela.order.dto.OrderDtos.CreateOrderRequest;
import com.dolcecandela.order.dto.OrderDtos.DeliveryRequest;
import com.dolcecandela.order.dto.OrderDtos.OrderItemRequest;
import com.dolcecandela.storage.FileStorageService;
import com.dolcecandela.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests unitaires du service de commandes : la base de données est remplacée par des mocks Mockito. */
class OrderServiceTest {

    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private OrderService service;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties(
                new AppProperties.Jwt("x".repeat(32), 60),
                new AppProperties.Cors(List.of()),
                new AppProperties.Admin(null, null, null),
                new AppProperties.Business(new BigDecimal("8.000"), 30, 10, "Centre-ville de Monastir"),
                new AppProperties.Storage("target/x"),
                new AppProperties.Security(5, 15));
        orderRepository = mock(OrderRepository.class);
        productRepository = mock(ProductRepository.class);
        service = new OrderService(orderRepository, productRepository, mock(CustomizationOptionRepository.class),
                mock(UserRepository.class), new PricingService(properties), mock(FileStorageService.class), properties);
    }

    @Test
    void inactiveProductCannotBeOrdered() {
        Product product = product(5);
        product.setActive(false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        CreateOrderRequest request = new CreateOrderRequest("Client", "22333444", null,
                List.of(new OrderItemRequest(1L, 1, null, null)),
                new DeliveryRequest(DeliveryMethod.PICKUP, null, null, null, null), null);

        assertThatThrownBy(() -> service.createOrder(request, null))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void forbiddenTransitionIsRejected() {
        Order order = order(OrderStatus.PENDING, product(5), 1);
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.updateStatus(7L, OrderStatus.DELIVERED))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void cancellingRestoresStock() {
        Product product = product(5);
        Order order = order(OrderStatus.CONFIRMED, product, 2);
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        service.updateStatus(7L, OrderStatus.CANCELLED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.getStockQuantity()).isEqualTo(7);
        assertThat(order.getDelivery().getStatus()).isEqualTo(DeliveryStatus.FAILED);
    }

    private static Product product(int stock) {
        Product product = new Product();
        product.setNameFr("Bougie test");
        product.setPrice(new BigDecimal("42.000"));
        product.setStockQuantity(stock);
        product.setActive(true);
        return product;
    }

    private static Order order(OrderStatus status, Product product, int quantity) {
        Order order = new Order();
        order.setStatus(status);
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setProductName(product.getNameFr());
        item.setUnitPrice(product.getPrice());
        item.setQuantity(quantity);
        order.addItem(item);
        Delivery delivery = new Delivery();
        delivery.setMethod(DeliveryMethod.PICKUP);
        order.attachDelivery(delivery);
        return order;
    }
}
