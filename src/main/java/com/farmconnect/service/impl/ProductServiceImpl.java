package com.farmconnect.service.impl;

import com.farmconnect.dto.request.ProductRequest;
import com.farmconnect.dto.response.ProductResponse;
import com.farmconnect.entity.Farmer;
import com.farmconnect.entity.Product;
import com.farmconnect.exception.BadRequestException;
import com.farmconnect.exception.ResourceNotFoundException;
import com.farmconnect.repository.FarmerRepository;
import com.farmconnect.repository.ProductRepository;
import com.farmconnect.repository.ReviewRepository;
import com.farmconnect.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl {

    private final ProductRepository productRepository;
    private final FarmerRepository farmerRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public ProductResponse addProduct(ProductRequest req) {
        Farmer farmer = getCurrentVerifiedFarmer();

        Product product = Product.builder()
                .farmer(farmer)
                .productName(req.getProductName())
                .description(req.getDescription())
                .category(req.getCategory())
                .price(req.getPrice())
                .unit(req.getUnit())
                .quantity(req.getQuantity())
                .isOrganic(req.getIsOrganic() != null && req.getIsOrganic())
                .availability(req.getAvailability() == null || req.getAvailability())
                .build();

        // Zero/no stock always wins over any requested availability=true -
        // you cannot sell what you don't have.
        if (product.getQuantity() == null || product.getQuantity() <= 0) {
            product.setAvailability(false);
        }

        product = productRepository.save(product);
        return toResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest req) {
        Product product = getOwnedProduct(productId);
        product.setProductName(req.getProductName());
        product.setDescription(req.getDescription());
        product.setCategory(req.getCategory());
        product.setPrice(req.getPrice());
        product.setUnit(req.getUnit());
        product.setQuantity(req.getQuantity());
        if (req.getIsOrganic() != null) product.setIsOrganic(req.getIsOrganic());
        if (req.getAvailability() != null) product.setAvailability(req.getAvailability());

        // Same rule as addProduct: zero/no stock forces unavailable
        // regardless of what availability value was requested.
        if (product.getQuantity() == null || product.getQuantity() <= 0) {
            product.setAvailability(false);
        }

        return toResponse(productRepository.save(product));
    }

    @Transactional
public void deleteProduct(Long productId) {
    Product product = getOwnedProduct(productId);
    productRepository.delete(product);
}

@Transactional
public ProductResponse setImage(Long productId, String imageUrl) {
    Product product = getOwnedProduct(productId);
    product.setImageUrl(imageUrl);
    return toResponse(productRepository.save(product));
}

@Transactional
public ProductResponse toggleAvailability(Long productId) {
    Product product = getOwnedProduct(productId);

    Boolean currentAvailability = product.getAvailability();
    product.setAvailability(
            currentAvailability == null || !currentAvailability
    );

    return toResponse(productRepository.save(product));
}

@Transactional(readOnly = true)
public ProductResponse getProduct(Long productId) {
    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    return toResponse(product);
}

@Transactional(readOnly = true)
public List<ProductResponse> getAllAvailable() {
    return productRepository.findByAvailabilityTrue()
            .stream()
            .map(this::toResponse)
            .toList();
}

@Transactional(readOnly = true)
public List<ProductResponse> adminAllProducts() {
    return productRepository.findAll()
            .stream()
            .map(this::toResponse)
            .toList();
}

@Transactional(readOnly = true)
public List<ProductResponse> adminProductsByFarmer(Long farmerId) {
    return productRepository.findByFarmer_FarmerId(farmerId)
            .stream()
            .map(this::toResponse)
            .toList();
}

@Transactional(readOnly = true)
public List<ProductResponse> getByCategory(Product.Category category) {
    return productRepository.findByCategoryAndAvailabilityTrue(category)
            .stream()
            .map(this::toResponse)
            .toList();
}

@Transactional(readOnly = true)
public List<ProductResponse> getOrganic() {
    return productRepository.findByIsOrganicTrueAndAvailabilityTrue()
            .stream()
            .map(this::toResponse)
            .toList();
}

@Transactional(readOnly = true)
public List<ProductResponse> search(String keyword) {
    return productRepository.searchByKeyword(keyword)
            .stream()
            .map(this::toResponse)
            .toList();
}

@Transactional(readOnly = true)
public List<ProductResponse> recentlyAdded() {
    return productRepository.findTop10ByAvailabilityTrueOrderByCreatedAtDesc()
            .stream()
            .map(this::toResponse)
            .toList();
}

@Transactional(readOnly = true)
public List<ProductResponse> myProducts() {
    Farmer farmer = farmerRepository.findByUser_UserId(SecurityUtil.currentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found"));

    return productRepository.findByFarmer_FarmerId(farmer.getFarmerId())
            .stream()
            .map(this::toResponse)
            .toList();
}

    // ---- helpers ----

    private Farmer getCurrentVerifiedFarmer() {
        Farmer farmer = farmerRepository.findByUser_UserId(SecurityUtil.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found"));
        if (farmer.getVerificationStatus() != Farmer.VerificationStatus.VERIFIED) {
            throw new BadRequestException("Your farmer account is not yet verified by admin. You cannot manage products until verified.");
        }
        return farmer;
    }

    private Product getOwnedProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        Long currentUserId = SecurityUtil.currentUserId();
        if (!product.getFarmer().getUser().getUserId().equals(currentUserId)) {
            throw new BadRequestException("You do not own this product");
        }
        return product;
    }

    private ProductResponse toResponse(Product p) {
        Double avgRating = reviewRepository.findAverageRatingByProductId(p.getProductId());
        int reviewCount = reviewRepository.findByProduct_ProductIdOrderByCreatedAtDesc(p.getProductId()).size();
        return ProductResponse.builder()
                .productId(p.getProductId())
                .farmerId(p.getFarmer().getFarmerId())
                .farmName(p.getFarmer().getFarmName())
                .farmerCity(p.getFarmer().getUser().getCity())
                .productName(p.getProductName())
                .description(p.getDescription())
                .category(p.getCategory())
                .price(p.getPrice())
                .unit(p.getUnit())
                .quantity(p.getQuantity())
                .imageUrl(p.getImageUrl())
                .isOrganic(p.getIsOrganic())
                .availability(p.getAvailability())
                .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
                .reviewCount(reviewCount)
                .createdAt(p.getCreatedAt())
                .build();
    }
}
