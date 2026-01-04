package com.tulip.repository;

import com.tulip.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Interface này quản lý bảng User, và khóa chính của bảng đó kiểu Long
@Repository // Có hay ko cũng đc vì đã extends JpaRepository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.profile ORDER BY u.createdAt DESC")
    List<User> findAllWithProfile();

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.profile p " +
            "WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR p.phone LIKE CONCAT('%', :keyword, '%') " +
            "ORDER BY u.createdAt DESC")
    List<User> searchWithProfile(@Param("keyword") String keyword);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.profile WHERE u.role = :role ORDER BY u.createdAt DESC")
    List<User> findByRoleWithProfile(@Param("role") com.tulip.entity.Role role);
}
