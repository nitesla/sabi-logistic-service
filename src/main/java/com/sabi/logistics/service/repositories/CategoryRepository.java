package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Category findByName(String name);
    Category findCategoriesById(Long id);
    List<Category> findByIsActive(Boolean isActive);


    @Query("SELECT c FROM Category c WHERE ((:name IS NULL) OR (:name IS NOT NULL AND c.name like %:name%)) order by c.id desc")
    Page<Category> findCategories(@Param("name") String name,
                                  Pageable pageable);
}
