package com.farmconnect.service.impl;

import com.farmconnect.dto.response.DashboardResponse;
import com.farmconnect.entity.Farmer;
import com.farmconnect.entity.Order;
import com.farmconnect.entity.Product;
import com.farmconnect.entity.User;
import com.farmconnect.exception.ResourceNotFoundException;
import com.farmconnect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl {

    private final UserRepository userRepository;
    private final FarmerRepository farmerRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public DashboardResponse getDashboard() {
        List<Order> delivered = orderRepository.findByOrderStatus(Order.OrderStatus.DELIVERED);
        BigDecimal revenue = delivered.stream().map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardResponse.builder()
                .totalUsers(userRepository.count())
                .totalFarmers(userRepository.countByRole(User.Role.FARMER))
                .totalCustomers(userRepository.countByRole(User.Role.CUSTOMER))
                .totalProducts(productRepository.count())
                .totalOrders(orderRepository.count())
                .pendingFarmerVerifications(farmerRepository.findByVerificationStatus(Farmer.VerificationStatus.PENDING).size())
                .totalRevenue(revenue)
                .build();
    }

    public List<User> allUsers() {
        return userRepository.findAll();
    }

    public List<User> allCustomers() {
        return userRepository.findByRole(User.Role.CUSTOMER);
    }

    public List<Farmer> allFarmers() {
        return farmerRepository.findAll();
    }

    public void deactivateUser(Long userId) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setIsActive(false);
            userRepository.save(u);
        });
    }

    public void activateUser(Long userId) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setIsActive(true);
            userRepository.save(u);
        });
    }

    /**
     * Blocks a farmer: disables their account (they can no longer log in -
     * see AuthServiceImpl#login / UserPrincipal#isEnabled) and hides every
     * one of their products from customer browsing.
     *
     * This intentionally does NOT hard-delete the farmer row or their
     * products: existing orders reference those products via OrderItem, so
     * deleting them would either violate that foreign key or silently erase
     * order history. Deactivating + hiding achieves "this farmer can no
     * longer sell or log in" without corrupting past order data.
     */
    @Transactional
    public void blockFarmer(Long farmerId) {
        Farmer farmer = farmerRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found"));

        farmer.getUser().setIsActive(false);
        userRepository.save(farmer.getUser());

        List<Product> products = productRepository.findByFarmer_FarmerId(farmerId);
        for (Product p : products) {
            p.setAvailability(false);
        }
        productRepository.saveAll(products);
    }

    /**
     * Blocks a customer: disables their account so they can no longer log
     * in. Their existing reviews and orders are left in place (deleting
     * them would corrupt order/review history and other users' visibility
     * into product ratings); the login block is what actually stops abuse.
     */
    @Transactional
    public void blockCustomer(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (user.getRole() != User.Role.CUSTOMER) {
            throw new ResourceNotFoundException("Customer not found");
        }
        user.setIsActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void unblockFarmer(Long farmerId) {
        Farmer farmer = farmerRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found"));

        farmer.getUser().setIsActive(true);
        userRepository.save(farmer.getUser());

        List<Product> products = productRepository.findByFarmer_FarmerId(farmerId);
        for (Product p : products) {
            p.setAvailability(true);
        }
        productRepository.saveAll(products);
    }

    @Transactional
    public void unblockCustomer(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (user.getRole() != User.Role.CUSTOMER) {
            throw new ResourceNotFoundException("Customer not found");
        }
        user.setIsActive(true);
        userRepository.save(user);
    }
}
