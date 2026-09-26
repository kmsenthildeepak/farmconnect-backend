package com.farmconnect.service.impl;

import com.farmconnect.dto.request.ReviewRequest;
import com.farmconnect.dto.response.ReviewResponse;
import com.farmconnect.entity.Product;
import com.farmconnect.entity.Review;
import com.farmconnect.entity.User;
import com.farmconnect.exception.BadRequestException;
import com.farmconnect.exception.ResourceNotFoundException;
import com.farmconnect.repository.*;
import com.farmconnect.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public ReviewResponse addReview(ReviewRequest req) {
        Long customerId = SecurityUtil.currentUserId();
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        boolean hasDeliveredOrder = orderItemRepository
                .existsByOrder_Customer_UserIdAndProduct_ProductIdAndOrder_OrderStatus(
                        customerId, product.getProductId(), com.farmconnect.entity.Order.OrderStatus.DELIVERED);

        if (!hasDeliveredOrder) {
            throw new BadRequestException("You can only review products from orders that have been delivered to you");
        }

        Review review = Review.builder()
                .customer(customer)
                .product(product)
                .farmer(product.getFarmer())
                .rating(req.getRating())
                .comment(req.getComment())
                .build();
        review = reviewRepository.save(review);
        return toResponse(review);
    }

    @Transactional(readOnly = true)
public List<ReviewResponse> getProductReviews(Long productId) {
    return reviewRepository.findByProduct_ProductIdOrderByCreatedAtDesc(productId)
            .stream()
            .map(this::toResponse)
            .toList();
}

    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews() {
        Long customerId = SecurityUtil.currentUserId();
        return reviewRepository.findByCustomer_UserIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * A customer may only edit/delete their OWN review - ownership is
     * checked against the authenticated customer, never trusted from the
     * client, so customer A cannot edit/delete customer B's review by
     * guessing a review id.
     */
    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewRequest req) {
        Review review = getOwnedReview(reviewId);
        review.setRating(req.getRating());
        review.setComment(req.getComment());
        review = reviewRepository.save(review);
        return toResponse(review);
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = getOwnedReview(reviewId);
        reviewRepository.delete(review);
    }

    private Review getOwnedReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (!review.getCustomer().getUserId().equals(SecurityUtil.currentUserId())) {
            throw new ResourceNotFoundException("Review not found");
        }
        return review;
    }

    private ReviewResponse toResponse(Review r) {
        return ReviewResponse.builder()
                .reviewId(r.getReviewId())
                .productId(r.getProduct().getProductId())
                .productName(r.getProduct().getProductName())
                .productImage(r.getProduct().getImageUrl())
                .customerName(r.getCustomer().getName())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
