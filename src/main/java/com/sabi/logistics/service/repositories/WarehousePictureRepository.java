package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.WarehousePicture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface WarehousePictureRepository extends JpaRepository<WarehousePicture, Long> {

    List<WarehousePicture> findByIsActive(Boolean isActive);


    @Query("SELECT c FROM WarehousePicture c WHERE ((:warehouseId IS NULL) OR (:warehouseId IS NOT NULL AND c.warehouseId = :warehouseId)) order by c.id desc ")
    Page<WarehousePicture> findWarehousePicture(@Param("warehouseId") Long warehouseId,
                                Pageable pageable);

}
