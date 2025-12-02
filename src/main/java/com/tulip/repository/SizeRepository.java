package com.tulip.repository;
import com.tulip.entity.product.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface SizeRepository extends JpaRepository<Size, Integer> {


}