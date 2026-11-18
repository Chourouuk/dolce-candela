package com.dolcecandela.order;

import com.dolcecandela.payment.PaymentEnums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByReference(String reference);

    boolean existsByReference(String reference);

    Page<Order> findAllByUser_Id(Long userId, Pageable pageable);

    Page<Order> findAllByStatus(OrderStatus status, Pageable pageable);

    long countByStatus(OrderStatus status);

    @Query("select coalesce(sum(o.total), 0) from Order o where o.status = :status")
    BigDecimal sumTotalByStatus(@Param("status") OrderStatus status);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = :status")
    BigDecimal sumPaymentsByStatus(@Param("status") PaymentStatus status);

    /** Achat vérifié : l'utilisateur a reçu au moins une commande contenant ce produit. */
    @Query("""
            select count(i) from OrderItem i
            where i.order.user.id = :userId
              and i.product.id = :productId
              and i.order.status = :status
            """)
    long countPurchases(@Param("userId") Long userId,
                        @Param("productId") Long productId,
                        @Param("status") OrderStatus status);
}
