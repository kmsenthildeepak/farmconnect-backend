package com.farmconnect.repository;

import com.farmconnect.entity.Farmer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FarmerRepository extends JpaRepository<Farmer, Long> {
    Optional<Farmer> findByUser_UserId(Long userId);
    List<Farmer> findByVerificationStatus(Farmer.VerificationStatus status);
    Optional<Farmer> findByUser_EmailIgnoreCase(String email);
}
