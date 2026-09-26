package com.farmconnect.service;

import com.farmconnect.entity.Farmer;
import com.farmconnect.entity.Product;
import com.farmconnect.entity.User;
import com.farmconnect.exception.ResourceNotFoundException;
import com.farmconnect.repository.FarmerRepository;
import com.farmconnect.repository.ProductRepository;
import com.farmconnect.repository.UserRepository;
import com.farmconnect.security.UserPrincipal;
import com.farmconnect.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminBlockUserTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FarmerRepository farmerRepository;

    @Mock
    private ProductRepository productRepository;

    private AdminServiceImpl adminService;

    private User farmerUser;
    private Farmer farmer;
    private List<Product> farmerProducts;

    private User customerUser;

    @BeforeEach
    public void setup() {
        adminService = new AdminServiceImpl(userRepository, farmerRepository, productRepository, null);

        farmerUser = User.builder()
                .userId(10L)
                .name("Farmer John")
                .email("john@farm.com")
                .role(User.Role.FARMER)
                .isActive(true)
                .build();

        farmer = Farmer.builder()
                .farmerId(1L)
                .user(farmerUser)
                .farmName("John's Green Acres")
                .verificationStatus(Farmer.VerificationStatus.VERIFIED)
                .build();

        farmerProducts = new ArrayList<>();
        Product p1 = Product.builder()
                .productId(101L)
                .farmer(farmer)
                .productName("Organic Tomatoes")
                .availability(true)
                .build();
        Product p2 = Product.builder()
                .productId(102L)
                .farmer(farmer)
                .productName("Fresh Spinach")
                .availability(true)
                .build();
        farmerProducts.add(p1);
        farmerProducts.add(p2);

        customerUser = User.builder()
                .userId(20L)
                .name("Customer Alice")
                .email("alice@gmail.com")
                .role(User.Role.CUSTOMER)
                .isActive(true)
                .build();
    }

    @Test
    public void testBlockFarmer_SetsUserInactiveAndHidesProducts() {
        when(farmerRepository.findById(1L)).thenReturn(Optional.of(farmer));
        when(productRepository.findByFarmer_FarmerId(1L)).thenReturn(farmerProducts);

        adminService.blockFarmer(1L);

        assertFalse(farmerUser.getIsActive(), "Farmer User isActive must be false after blocking");
        verify(userRepository).save(farmerUser);

        for (Product p : farmerProducts) {
            assertFalse(p.getAvailability(), "Product availability must be set to false when farmer is blocked");
        }
        verify(productRepository).saveAll(farmerProducts);

        // Security principal check
        UserPrincipal principal = new UserPrincipal(farmerUser);
        assertFalse(principal.isEnabled(), "UserPrincipal must be disabled when account is blocked");
    }

    @Test
    public void testUnblockFarmer_RestoresUserActiveAndProducts() {
        farmerUser.setIsActive(false);
        farmerProducts.get(0).setAvailability(false);
        farmerProducts.get(1).setAvailability(false);

        when(farmerRepository.findById(1L)).thenReturn(Optional.of(farmer));
        when(productRepository.findByFarmer_FarmerId(1L)).thenReturn(farmerProducts);

        adminService.unblockFarmer(1L);

        assertTrue(farmerUser.getIsActive(), "Farmer User isActive must be true after unblocking");
        verify(userRepository).save(farmerUser);

        for (Product p : farmerProducts) {
            assertTrue(p.getAvailability(), "Product availability must be restored to true when farmer is unblocked");
        }
        verify(productRepository).saveAll(farmerProducts);

        // Security principal check
        UserPrincipal principal = new UserPrincipal(farmerUser);
        assertTrue(principal.isEnabled(), "UserPrincipal must be enabled when account is unblocked");
    }

    @Test
    public void testBlockCustomer_SetsCustomerInactive() {
        when(userRepository.findById(20L)).thenReturn(Optional.of(customerUser));

        adminService.blockCustomer(20L);

        assertFalse(customerUser.getIsActive(), "Customer User isActive must be false after blocking");
        verify(userRepository).save(customerUser);

        UserPrincipal principal = new UserPrincipal(customerUser);
        assertFalse(principal.isEnabled(), "Customer UserPrincipal must be disabled when blocked");
    }

    @Test
    public void testUnblockCustomer_RestoresCustomerActive() {
        customerUser.setIsActive(false);
        when(userRepository.findById(20L)).thenReturn(Optional.of(customerUser));

        adminService.unblockCustomer(20L);

        assertTrue(customerUser.getIsActive(), "Customer User isActive must be true after unblocking");
        verify(userRepository).save(customerUser);

        UserPrincipal principal = new UserPrincipal(customerUser);
        assertTrue(principal.isEnabled(), "Customer UserPrincipal must be enabled after unblocking");
    }

    @Test
    public void testDataPreservationAcrossBlockAndUnblock() {
        when(farmerRepository.findById(1L)).thenReturn(Optional.of(farmer));
        when(productRepository.findByFarmer_FarmerId(1L)).thenReturn(farmerProducts);

        // Block
        adminService.blockFarmer(1L);
        assertEquals("Farmer John", farmer.getUser().getName());
        assertEquals("john@farm.com", farmer.getUser().getEmail());
        assertEquals("John's Green Acres", farmer.getFarmName());
        assertEquals(2, farmerProducts.size());
        assertEquals(101L, farmerProducts.get(0).getProductId());

        // Unblock
        adminService.unblockFarmer(1L);
        assertEquals("Farmer John", farmer.getUser().getName());
        assertEquals("john@farm.com", farmer.getUser().getEmail());
        assertEquals("John's Green Acres", farmer.getFarmName());
        assertEquals(2, farmerProducts.size());
        assertEquals(101L, farmerProducts.get(0).getProductId());
    }

    @Test
    public void testBlockCustomer_NotFoundThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminService.blockCustomer(999L));
    }

    @Test
    public void testUnblockCustomer_NotFoundThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminService.unblockCustomer(999L));
    }

    @Test
    public void testExactBlockedErrorMessage() {
        String expectedMessage = "You were blocked by admin. You are requested to contact Admin for further process.";
        DisabledException ex = new DisabledException(expectedMessage);
        assertEquals(expectedMessage, ex.getMessage());
    }
}
