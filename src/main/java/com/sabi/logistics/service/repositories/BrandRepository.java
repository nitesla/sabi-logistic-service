package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    Brand findByName(String name);

    Brand findBrandById(Long Id);

    @Query("SELECT c FROM Brand c WHERE ((:name IS NULL) OR (:name IS NOT NULL AND c.name like %:name%)) order by c.id desc")
    Page<Brand> findBrand(String name, Pageable pageRequest);

    List<Brand> findByIsActive(Boolean isActive);
}
