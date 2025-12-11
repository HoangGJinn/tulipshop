package com.tulip.repository;

import com.tulip.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    List<UserAddress> findByProfileIdOrderByIsDefaultDescIdDesc(Long profileId);
    
    Optional<UserAddress> findByProfileIdAndIsDefaultTrue(Long profileId);
    
    long countByProfileId(Long profileId);
    
    boolean existsByIdAndProfileId(Long addressId, Long profileId);
    
    // Bỏ đặt tất cả địa chỉ mặc định của profile
    @Modifying
    @Query("UPDATE UserAddress ua SET ua.isDefault = false WHERE ua.profile.id = :profileId")
    void unsetAllDefaultAddresses(@Param("profileId") Long profileId);
}

