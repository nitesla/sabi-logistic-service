package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    @Query("SELECT s FROM Warehouse s WHERE ((:owner IS NULL) OR (:owner IS NOT NULL AND s.owner = :owner))" +
            " AND ((:name IS NULL) OR (:name IS NOT NULL AND s.name = :name))" +
            " AND ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND s.partnerId = :partnerId))"+
            " AND ((:lgaId IS NULL) OR (:lgaId IS NOT NULL AND s.lgaId = :lgaId))")
    Page<Warehouse> findWarehouse(@Param("owner") String owner,
                                  @Param("name") String name,
                                  @Param("partnerId") Long partnerId,
                                  @Param("lgaId") Long lgaId,
                                  Pageable pageable);
    Warehouse findWarehouseById (Long id);

    List<Warehouse> findByIsActive(Boolean isActive);

    Warehouse findByUserId(Long userId);
}
