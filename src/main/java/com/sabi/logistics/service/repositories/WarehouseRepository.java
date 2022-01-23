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
    @Query("SELECT s FROM Warehouse s WHERE ((:owner IS NULL) OR (:owner IS NOT NULL AND s.owner like %:owner%))" +
            " AND ((:name IS NULL) OR (:name IS NOT NULL AND s.name like %:name%))" +
            " AND ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND s.partnerId = :partnerId))"+
            " AND ((:isActive IS NULL) OR (:isActive IS NOT NULL AND s.isActive = :isActive))"+
            " AND ((:lgaId IS NULL) OR (:lgaId IS NOT NULL AND s.lgaId = :lgaId)) order by s.id desc")
    Page<Warehouse> findWarehouse(@Param("owner") String owner,
                                  @Param("name") String name,
                                  @Param("partnerId") Long partnerId,
                                  @Param("isActive") Boolean isActive,
                                  @Param("lgaId") Long lgaId,
                                  Pageable pageable);
    Warehouse findWarehouseById (Long id);

    @Query("SELECT s FROM Warehouse s WHERE ((:isActive IS NULL) OR (:isActive IS NOT NULL AND s.isActive = :isActive))" +
            " AND ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND s.partnerId = :partnerId))")
    List<Warehouse> findWarehouses(Boolean isActive,Long partnerId);

    Warehouse findByPartnerId(Long partnerId);
}
