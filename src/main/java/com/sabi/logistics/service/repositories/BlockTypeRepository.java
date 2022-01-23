package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.BlockType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockTypeRepository extends JpaRepository<BlockType, Long> {

    BlockType findByName(String name);
    List<BlockType> findByPrice(double price);
    List<BlockType> findByIsActive(Boolean isActive);

    @Query("SELECT c FROM BlockType c WHERE ((:name IS NULL) OR (:name IS NOT NULL AND c.name like %:name%)) order by c.id desc" )
//            " AND ((:length IS NULL) OR (:length IS NOT NULL AND c.length = :length))" +
//            " AND ((:width IS NULL) OR (:width IS NOT NULL AND c.width = :width))" +
//            " AND ((:height IS NULL) OR (:height IS NOT NULL AND c.height = :height))" +
//            " AND ((:price >= 0.0) OR (:price  AND c.price = :price))" )
//            " AND ((:isActive IS NULL) OR (:isActive IS NOT NULL AND c.isActive = :isActive))")
    Page<BlockType> findAllBlockType(@Param("name") String name,
//                                  @Param("price") double price,
                                  Pageable pageable);
}
