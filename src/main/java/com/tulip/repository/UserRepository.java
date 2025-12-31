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

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.profile")
    List<User> findAllWithProfile();

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.profile p " +
            "WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchWithProfile(@Param("keyword") String keyword);
}
