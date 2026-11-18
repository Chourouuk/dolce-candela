package com.dolcecandela.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findAllByProduct_IdAndApprovedTrue(Long productId, Pageable pageable);

    Page<Review> findAllByApprovedFalse(Pageable pageable);

    boolean existsByProduct_IdAndUser_Id(Long productId, Long userId);

    long countByProduct_IdAndApprovedTrue(Long productId);

    long countByApprovedFalse();

    @Query("select avg(r.rating) from Review r where r.product.id = :productId and r.approved = true")
    Double averageRating(@Param("productId") Long productId);
}
