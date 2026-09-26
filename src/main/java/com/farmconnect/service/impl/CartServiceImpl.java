package com.farmconnect.service.impl;

import com.farmconnect.dto.request.CartItemRequest;
import com.farmconnect.dto.response.CartResponse;
import com.farmconnect.entity.Cart;
import com.farmconnect.entity.CartItem;
import com.farmconnect.entity.Product;
import com.farmconnect.entity.User;
import com.farmconnect.exception.BadRequestException;
import com.farmconnect.exception.ResourceNotFoundException;
import com.farmconnect.repository.CartItemRepository;
import com.farmconnect.repository.CartRepository;
import com.farmconnect.repository.ProductRepository;
import com.farmconnect.repository.UserRepository;
import com.farmconnect.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl {

private final CartRepository cartRepository;
private final CartItemRepository cartItemRepository;
private final ProductRepository productRepository;
private final UserRepository userRepository;

// =========================================================
// ADD ITEM
// =========================================================

@Transactional
public CartResponse addItem(CartItemRequest req) {

    if (req == null) {
        throw new BadRequestException("Cart item request is required");
    }

    if (req.getQuantity() <= 0) {
        throw new BadRequestException(
                "Quantity must be greater than zero"
        );
    }

    Cart cart = getOrCreateCart();

    Product product = productRepository
            .findById(req.getProductId())
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Product not found"
                    )
            );

    if (!Boolean.TRUE.equals(product.getAvailability())) {
        throw new BadRequestException(
                "This product is currently unavailable"
        );
    }

    Integer stock = product.getQuantity();

    if (stock == null || stock <= 0) {
        throw new BadRequestException(
                "This product is currently out of stock"
        );
    }

    var existing =
            cartItemRepository
                    .findByCart_CartIdAndProduct_ProductId(
                            cart.getCartId(),
                            product.getProductId()
                    );

    int newQuantity = req.getQuantity();

    if (existing.isPresent()) {
        Integer existingQuantity =
                existing.get().getQuantity();

        if (existingQuantity != null) {
            newQuantity += existingQuantity;
        }
    }

    if (newQuantity > stock) {

    String unit =
            product.getUnit() == null
                    ? "units"
                    : product.getUnit().toString();

    throw new BadRequestException(
            "Only " + stock + " " + unit
                    + " available in stock"
    );
}

    if (existing.isPresent()) {

        CartItem item = existing.get();

        item.setQuantity(newQuantity);

        cartItemRepository.save(item);

    } else {

        CartItem item =
                CartItem.builder()
                        .cart(cart)
                        .product(product)
                        .quantity(newQuantity)
                        .build();

        cartItemRepository.save(item);
    }

    return getCart();
}

// =========================================================
// UPDATE QUANTITY
// =========================================================

@Transactional
public CartResponse updateItemQuantity(
        Long cartItemId,
        int quantity
) {

    if (cartItemId == null) {
        throw new BadRequestException(
                "Cart item ID is required"
        );
    }

    CartItem item =
            cartItemRepository
                    .findById(cartItemId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Cart item not found"
                            )
                    );

    ensureOwnedByCurrentUser(item);

    /*
     * Quantity <= 0 means remove the item.
     */
    if (quantity <= 0) {

        cartItemRepository.delete(item);

        return getCart();
    }

    Product product = item.getProduct();

    if (product == null) {
        throw new ResourceNotFoundException(
                "Product associated with cart item was not found"
        );
    }

    Integer stock = product.getQuantity();

    if (stock == null || stock <= 0) {
        throw new BadRequestException(
                "This product is currently out of stock"
        );
    }

    if (quantity > stock) {

    String unit =
            product.getUnit() == null
                    ? "units"
                    : product.getUnit().toString();

    throw new BadRequestException(
            "Only " + stock + " " + unit
                    + " available in stock"
    );
}

    if (!Boolean.TRUE.equals(product.getAvailability())) {
        throw new BadRequestException(
                "This product is currently unavailable"
        );
    }

    item.setQuantity(quantity);

    cartItemRepository.save(item);

    return getCart();
}

// =========================================================
// REMOVE ITEM
// =========================================================

@Transactional
public CartResponse removeItem(Long cartItemId) {

    if (cartItemId == null) {
        throw new BadRequestException(
                "Cart item ID is required"
        );
    }

    CartItem item =
            cartItemRepository
                    .findById(cartItemId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Cart item not found"
                            )
                    );

    ensureOwnedByCurrentUser(item);

    Cart cart = item.getCart();

    if (cart == null) {
        throw new ResourceNotFoundException(
                "Cart associated with cart item was not found"
        );
    }

    /*
     * Remove the item from the in-memory Cart collection first.
     *
     * Cart has:
     * @OneToMany(mappedBy = "cart",
     *            cascade = CascadeType.ALL,
     *            orphanRemoval = true)
     *
     * Keeping both sides synchronized prevents getCart()
     * from returning the deleted item while this transaction
     * is still active.
     */
    cart.getCartItems().remove(item);

    /*
     * Explicitly delete the database row.
     */
    cartItemRepository.delete(item);

    /*
     * Force Hibernate/JPA to execute the DELETE before
     * getCart() reads the cart again.
     */
    cartItemRepository.flush();

    /*
     * Make sure the Cart entity is synchronized as well.
     */
    cartRepository.save(cart);

    /*
     * Now rebuild the response after the DELETE has been
     * flushed and the in-memory collection has been updated.
     */
    return getCart();
}


// =========================================================
// CLEAR CART
// =========================================================

@Transactional
public void clearCart() {

    Cart cart = getOrCreateCart();

    /*
     * Explicitly delete the cart items instead of relying only
     * on collection.clear().
     *
     * This avoids problems where Hibernate/JPA does not issue
     * the expected DELETE statements for the child records.
     */
    List<CartItem> cartItems =
            new ArrayList<>(
                    cart.getCartItems()
            );

    if (!cartItems.isEmpty()) {

        cartItemRepository.deleteAll(
                cartItems
        );

        cartItemRepository.flush();
    }

    /*
     * Keep the in-memory entity consistent with the database.
     */
    cart.getCartItems().clear();

    cartRepository.save(cart);
}

// =========================================================
// GET CART
// =========================================================

@Transactional(readOnly = true)
public CartResponse getCart() {

    Cart cart = getOrCreateCart();

    List<CartResponse.CartItemResponse> items =
            new ArrayList<>();

    if (cart.getCartItems() != null) {

        for (CartItem cartItem :
                cart.getCartItems()) {

            if (cartItem == null) {
                continue;
            }

            Product product =
                    cartItem.getProduct();

            if (product == null) {
                continue;
            }

            BigDecimal price =
                    product.getPrice() == null
                            ? BigDecimal.ZERO
                            : product.getPrice();

            int quantity =
                    cartItem.getQuantity() == null
                            ? 0
                            : cartItem.getQuantity();

            BigDecimal subtotal =
                    price.multiply(
                            BigDecimal.valueOf(
                                    quantity
                            )
                    );

            int availableStock =
                    product.getQuantity() == null
                            ? 0
                            : product.getQuantity();

            items.add(
                    CartResponse.CartItemResponse
                            .builder()
                            .cartItemId(
                                    cartItem.getCartItemId()
                            )
                            .productId(
                                    product.getProductId()
                            )
                            .productName(
                                    product.getProductName()
                            )
                            .imageUrl(
                                    product.getImageUrl()
                            )
                            .price(price)
                            .quantity(quantity)
                            .subtotal(subtotal)
                            .availableStock(
                                    availableStock
                            )
                            .build()
            );
        }
    }

    BigDecimal total =
            items.stream()
                    .map(
                            CartResponse.CartItemResponse
                                    ::getSubtotal
                    )
                    .filter(
                            subtotal ->
                                    subtotal != null
                    )
                    .reduce(
                            BigDecimal.ZERO,
                            BigDecimal::add
                    );

    return CartResponse.builder()
            .cartId(cart.getCartId())
            .items(items)
            .total(total)
            .build();
}

// =========================================================
// GET OR CREATE CART
// =========================================================

private Cart getOrCreateCart() {

    Long userId =
            SecurityUtil.currentUserId();

    if (userId == null) {
        throw new BadRequestException(
                "Unable to determine current user"
        );
    }

    return cartRepository
            .findByCustomer_UserId(userId)
            .orElseGet(() -> {

                User user =
                        userRepository
                                .findById(userId)
                                .orElseThrow(() ->
                                        new ResourceNotFoundException(
                                                "User not found"
                                        )
                                );

                Cart newCart =
                        Cart.builder()
                                .customer(user)
                                .build();

                return cartRepository.save(
                        newCart
                );
            });
}

// =========================================================
// OWNERSHIP CHECK
// =========================================================

private void ensureOwnedByCurrentUser(
        CartItem item
) {

    if (item == null) {
        throw new ResourceNotFoundException(
                "Cart item not found"
        );
    }

    Cart cart =
            item.getCart();

    if (cart == null) {
        throw new BadRequestException(
                "Cart item is not associated with a cart"
        );
    }

    User customer =
            cart.getCustomer();

    if (customer == null) {
        throw new BadRequestException(
                "Cart does not have an associated customer"
        );
    }

    Long currentUserId =
            SecurityUtil.currentUserId();

    Long cartUserId =
            customer.getUserId();

    if (currentUserId == null
            || cartUserId == null
            || !cartUserId.equals(currentUserId)) {

        throw new BadRequestException(
                "This cart item does not belong to you"
        );
    }
}

}
