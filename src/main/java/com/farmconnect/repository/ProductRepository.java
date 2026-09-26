package com.farmconnect.repository;

import com.farmconnect.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByFarmer_FarmerId(Long farmerId);

    List<Product> findByCategoryAndAvailabilityTrue(Product.Category category);

    List<Product> findByAvailabilityTrue();

    List<Product> findByIsOrganicTrueAndAvailabilityTrue();

    @Query("SELECT p FROM Product p WHERE p.availability = true AND " +
           "(LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchByKeyword(@Param("keyword") String keyword);

    List<Product> findTop10ByAvailabilityTrueOrderByCreatedAtDesc();
}
