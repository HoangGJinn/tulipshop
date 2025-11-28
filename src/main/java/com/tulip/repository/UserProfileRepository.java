package com.tulip.repository;

import com.tulip.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// thừa kế các hàm của JpaRepository như .save
// .findById(id), .findAll(), .delete(profile), .count()
@Repository // Có hay ko cũng đc vì đã extends JpaRepository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}
