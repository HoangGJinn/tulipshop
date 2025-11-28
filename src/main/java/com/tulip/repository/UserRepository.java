package com.tulip.repository;

import com.tulip.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// Interface này quản lý bảng User, và khóa chính của bảng đó kiểu Long
@Repository // Có hay ko cũng đc vì đã extends JpaRepository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
