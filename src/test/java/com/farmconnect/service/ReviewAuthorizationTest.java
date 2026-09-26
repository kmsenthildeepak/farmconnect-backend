package com.farmconnect.service;

import com.farmconnect.dto.request.ReviewRequest;
import com.farmconnect.dto.response.ReviewResponse;
import com.farmconnect.entity.*;
import com.farmconnect.exception.BadRequestException;
import com.farmconnect.exception.ResourceNotFoundException;
import com.farmconnect.repository.*;
import com.farmconnect.security.UserPrincipal;
import com.farmconnect.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewAuthorizationTest {

    private static final String EXACT_DELIVERED_MSG =
            "You can only review products from orders that have been delivered to you";

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    private ReviewServiceImpl reviewService;

    private User customerA;
    private User customerB;
    private Farmer farmer;
    private Product product1;
    private Product product2;

    @BeforeEach
    public void setup() {
        reviewService = new ReviewServiceImpl(
                reviewRepository,
                productRepository,
                userRepository,
                orderItemRepository
        );

        customerA = User.builder()
                .userId(100L)
                .name("Customer A")
                .email("customera@farmconnect.com")
                .role(User.Role.CUSTOMER)
                .isActive(true)
                .build();

        customerB = User.builder()
                .userId(200L)
                .name("Customer B")
                .email("customerb@farmconnect.com")
                .role(User.Role.CUSTOMER)
                .isActive(true)
                .build();

        farmer = Farmer.builder()
                .farmerId(10L)
                .farmName("Green Valley Farm")
                .user(User.builder().userId(50L).name("Farmer Bob").role(User.Role.FARMER).build())
                .build();

        product1 = Product.builder()
                .productId(1L)
                .productName("Organic Apples")
                .price(BigDecimal.valueOf(120.0))
                .farmer(farmer)
                .availability(true)
                .build();

        product2 = Product.builder()
                .productId(2L)
                .productName("Fresh Milk")
                .price(BigDecimal.valueOf(60.0))
                .farmer(farmer)
                .availability(true)
                .build();

        // Default: authenticate as Customer A
        setAuthenticatedUser(customerA);
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthenticatedUser(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // 1. DELIVERED order + requested product -> review succeeds
    @Test
    public void testAddReview_DeliveredOrder_Succeeds() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(customerA));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(orderItemRepository.existsByOrder_Customer_UserIdAndProduct_ProductIdAndOrder_OrderStatus(
                100L, 1L, Order.OrderStatus.DELIVERED)).thenReturn(true);

        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            r.setReviewId(555L);
            return r;
        });

        ReviewRequest req = new ReviewRequest();
        req.setProductId(1L);
        req.setRating(5);
        req.setComment("Delicious and fresh!");

        ReviewResponse response = reviewService.addReview(req);

        assertNotNull(response);
        assertEquals(555L, response.getReviewId());
        assertEquals(1L, response.getProductId());
        assertEquals("Organic Apples", response.getProductName());
        assertEquals("Customer A", response.getCustomerName());
        assertEquals(5, response.getRating());
        assertEquals("Delicious and fresh!", response.getComment());

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review saved = captor.getValue();
        assertEquals(100L, saved.getCustomer().getUserId(), "Review must be associated with authenticated customer A");
        assertEquals(1L, saved.getProduct().getProductId(), "Review must be associated with requested product");
        assertEquals(10L, saved.getFarmer().getFarmerId(), "Review must be associated with product farmer");
    }

    // 2. PENDING order + requested product -> review rejected
    @Test
    public void testAddReview_PendingOrder_ThrowsBadRequestException() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(customerA));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(orderItemRepository.existsByOrder_Customer_UserIdAndProduct_ProductIdAndOrder_OrderStatus(
                100L, 1L, Order.OrderStatus.DELIVERED)).thenReturn(false);

        ReviewRequest req = new ReviewRequest();
        req.setProductId(1L);
        req.setRating(4);
        req.setComment("Pending delivery review attempt");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> reviewService.addReview(req));
        assertEquals(EXACT_DELIVERED_MSG, ex.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    // 3. ACCEPTED order + requested product -> review rejected
    @Test
    public void testAddReview_AcceptedOrder_ThrowsBadRequestException() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(customerA));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(orderItemRepository.existsByOrder_Customer_UserIdAndProduct_ProductIdAndOrder_OrderStatus(
                100L, 1L, Order.OrderStatus.DELIVERED)).thenReturn(false);

        ReviewRequest req = new ReviewRequest();
        req.setProductId(1L);
        req.setRating(3);
        req.setComment("Accepted order review attempt");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> reviewService.addReview(req));
        assertEquals(EXACT_DELIVERED_MSG, ex.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    // 4. PACKED order + requested product -> review rejected
    @Test
    public void testAddReview_PackedOrder_ThrowsBadRequestException() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(customerA));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(orderItemRepository.existsByOrder_Customer_UserIdAndProduct_ProductIdAndOrder_OrderStatus(
                100L, 1L, Order.OrderStatus.DELIVERED)).thenReturn(false);

        ReviewRequest req = new ReviewRequest();
        req.setProductId(1L);
        req.setRating(4);
        req.setComment("Packed order review attempt");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> reviewService.addReview(req));
        assertEquals(EXACT_DELIVERED_MSG, ex.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    // 5. OUT_FOR_DELIVERY order + requested product -> review rejected
    @Test
    public void testAddReview_OutForDeliveryOrder_ThrowsBadRequestException() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(customerA));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(orderItemRepository.existsByOrder_Customer_UserIdAndProduct_ProductIdAndOrder_OrderStatus(
                100L, 1L, Order.OrderStatus.DELIVERED)).thenReturn(false);

        ReviewRequest req = new ReviewRequest();
        req.setProductId(1L);
        req.setRating(5);
        req.setComment("Out for delivery review attempt");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> reviewService.addReview(req));
        assertEquals(EXACT_DELIVERED_MSG, ex.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    // 6. REJECTED order + requested product -> review rejected
    @Test
    public void testAddReview_RejectedOrder_ThrowsBadRequestException() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(customerA));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(orderItemRepository.existsByOrder_Customer_UserIdAndProduct_ProductIdAndOrder_OrderStatus(
                100L, 1L, Order.OrderStatus.DELIVERED)).thenReturn(false);

        ReviewRequest req = new ReviewRequest();
        req.setProductId(1L);
        req.setRating(1);
        req.setComment("Rejected order review attempt");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> reviewService.addReview(req));
        assertEquals(EXACT_DELIVERED_MSG, ex.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    // 7. CANCELLED order + requested product -> review rejected
    @Test
    public void testAddReview_CancelledOrder_ThrowsBadRequestException() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(customerA));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(orderItemRepository.existsByOrder_Customer_UserIdAndProduct_ProductIdAndOrder_OrderStatus(
                100L, 1L, Order.OrderStatus.DELIVERED)).thenReturn(false);

        ReviewRequest req = new ReviewRequest();
        req.setProductId(1L);
        req.setRating(2);
        req.setComment("Cancelled order review attempt");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> reviewService.addReview(req));
        assertEquals(EXACT_DELIVERED_MSG, ex.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    // 8. Customer never purchased the requested product -> review rejected
    @Test
    public void testAddReview_NeverPurchased_ThrowsBadRequestException() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(customerA));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(orderItemRepository.existsByOrder_Customer_UserIdAndProduct_ProductIdAndOrder_OrderStatus(
                100L, 1L, Order.OrderStatus.DELIVERED)).thenReturn(false);

        ReviewRequest req = new ReviewRequest();
        req.setProductId(1L);
        req.setRating(5);
        req.setComment("Never purchased product review attempt");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> reviewService.addReview(req));
        assertEquals(EXACT_DELIVERED_MSG, ex.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    // 9. Customer purchased a different product -> review rejected
    @Test
    public void testAddReview_PurchasedDifferentProduct_ThrowsBadRequestException() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(customerA));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product2));
        // Customer A bought product 1, but attempts to review product 2
        when(orderItemRepository.existsByOrder_Customer_UserIdAndProduct_ProductIdAndOrder_OrderStatus(
                100L, 2L, Order.OrderStatus.DELIVERED)).thenReturn(false);

        ReviewRequest req = new ReviewRequest();
        req.setProductId(2L);
        req.setRating(4);
        req.setComment("Reviewing a product I did not buy");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> reviewService.addReview(req));
        assertEquals(EXACT_DELIVERED_MSG, ex.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    // 10. Customer A attempts to review using Customer B's delivered order -> review rejected
    @Test
    public void testAddReview_CustomerAAttemptsReviewWithCustomerBDeliveredOrder_ThrowsBadRequestException() {
        // Customer A (100L) is authenticated
        setAuthenticatedUser(customerA);

        when(userRepository.findById(100L)).thenReturn(Optional.of(customerA));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));

        // Customer B (200L) has delivered order, but Customer A (100L) does NOT
        when(orderItemRepository.existsByOrder_Customer_UserIdAndProduct_ProductIdAndOrder_OrderStatus(
                100L, 1L, Order.OrderStatus.DELIVERED)).thenReturn(false);

        ReviewRequest req = new ReviewRequest();
        req.setProductId(1L);
        req.setRating(5);
        req.setComment("Trying to review using someone else's order");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> reviewService.addReview(req));
        assertEquals(EXACT_DELIVERED_MSG, ex.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    // 11. Verify customer identity comes from SecurityContextHolder, not client
    @Test
    public void testAddReview_CustomerIdentityFromSecurityContext() {
        // Authenticated as Customer B (200L)
        setAuthenticatedUser(customerB);

        when(userRepository.findById(200L)).thenReturn(Optional.of(customerB));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(orderItemRepository.existsByOrder_Customer_UserIdAndProduct_ProductIdAndOrder_OrderStatus(
                200L, 1L, Order.OrderStatus.DELIVERED)).thenReturn(true);

        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            r.setReviewId(777L);
            return r;
        });

        ReviewRequest req = new ReviewRequest();
        req.setProductId(1L);
        req.setRating(5);
        req.setComment("Customer B review");

        ReviewResponse response = reviewService.addReview(req);

        assertNotNull(response);
        assertEquals("Customer B", response.getCustomerName());

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertEquals(200L, captor.getValue().getCustomer().getUserId(),
                "Customer identity must be resolved from SecurityContextHolder (200L)");
    }

    // 12. Ownership protection: update review belongs to another customer -> rejected
    @Test
    public void testUpdateReview_NonOwner_ThrowsResourceNotFoundException() {
        setAuthenticatedUser(customerA); // Customer A is 100L

        Review existingReview = Review.builder()
                .reviewId(99L)
                .customer(customerB) // Belongs to Customer B (200L)
                .product(product1)
                .rating(3)
                .comment("Original")
                .build();

        when(reviewRepository.findById(99L)).thenReturn(Optional.of(existingReview));

        ReviewRequest req = new ReviewRequest();
        req.setRating(5);
        req.setComment("Hacked comment");

        assertThrows(ResourceNotFoundException.class, () -> reviewService.updateReview(99L, req));
        verify(reviewRepository, never()).save(any());
    }

    // 13. Ownership protection: delete review belongs to another customer -> rejected
    @Test
    public void testDeleteReview_NonOwner_ThrowsResourceNotFoundException() {
        setAuthenticatedUser(customerA); // Customer A is 100L

        Review existingReview = Review.builder()
                .reviewId(99L)
                .customer(customerB) // Belongs to Customer B (200L)
                .product(product1)
                .rating(3)
                .comment("Original")
                .build();

        when(reviewRepository.findById(99L)).thenReturn(Optional.of(existingReview));

        assertThrows(ResourceNotFoundException.class, () -> reviewService.deleteReview(99L));
        verify(reviewRepository, never()).delete(any());
    }
}
