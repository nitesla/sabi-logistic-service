package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    @Query("SELECT s FROM Warehouse s WHERE ((:owner IS NULL) OR (:owner IS NOT NULL AND s.owner = :owner))")
    Page<Warehouse> findWarehouse(String owner, Pageable pageable);

    List<Warehouse> findByIsActive(Boolean isActive);
}
