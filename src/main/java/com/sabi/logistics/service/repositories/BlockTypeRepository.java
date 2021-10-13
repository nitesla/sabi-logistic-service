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

    @Query("SELECT c FROM BlockType c WHERE ((:name IS NULL) OR (:name IS NOT NULL AND c.name = :name))" +
            " AND ((:lengths IS NULL) OR (:lengths IS NOT NULL AND c.lengths = :lengths))" +
            " AND ((:width IS NULL) OR (:width IS NOT NULL AND c.width = :width))" +
            " AND ((:height IS NULL) OR (:height IS NOT NULL AND c.height = :height))" +
            " AND ((:price IS NULL) OR (:price IS NOT NULL AND c.price = :price))" )
//            " AND ((:isActive IS NULL) OR (:isActive IS NOT NULL AND c.isActive = :isActive))")
    Page<BlockType> findAllBlockType(@Param("name") String name,
                                  @Param("lengths") double lengths,
                                  @Param("width") double width,
                                  @Param("height") double height,
                                  @Param("price") double price,
                                  Pageable pageable);
}
