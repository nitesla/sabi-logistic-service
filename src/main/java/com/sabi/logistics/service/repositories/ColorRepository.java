package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.Color;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ColorRepository extends JpaRepository<Color, Long> {
    Color findByName(String name);

    Color findColorById(Long Id);

    @Query("SELECT c FROM Color c WHERE ((:name IS NULL) OR (:name IS NOT NULL AND c.name like %:name%)) order by c.id desc")
    Page<Color> findColor(String name, Pageable pageRequest);

    List<Color> findByIsActive(Boolean isActive);
}
