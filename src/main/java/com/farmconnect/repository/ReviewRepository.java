package com.farmconnect.repository;

import com.farmconnect.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProduct_ProductIdOrderByCreatedAtDesc(Long productId);
    List<Review> findByFarmer_FarmerId(Long farmerId);
    List<Review> findByCustomer_UserIdOrderByCreatedAtDesc(Long customerId);

    @org.springframework.data.jpa.repository.Query(
        "SELECT AVG(r.rating) FROM Review r WHERE r.product.productId = :productId")
    Double findAverageRatingByProductId(Long productId);
}
