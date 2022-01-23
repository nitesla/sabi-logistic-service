package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.Allocations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllocationsRepository extends JpaRepository<Allocations, Long> {

    Allocations findByName(String name);

    Allocations findAllocationsById(Long id);

    List<Allocations> findByIsActive(Boolean isActive);

    @Query("SELECT d FROM Allocations d WHERE ((:name IS NULL) OR (:name IS NOT NULL AND d.name = :name))" +
            " AND ((:wareHouseId IS NULL) OR (:wareHouseId IS NOT NULL AND d.wareHouseId = :wareHouseId))" +
            " AND ((:blockTypeId IS NULL) OR (:blockTypeId IS NOT NULL AND d.blockTypeId = :blockTypeId))" +
            " AND ((:status IS NULL) OR (:status IS NOT NULL AND d.status like %:status%))" +
            " AND ((:clientId IS NULL) OR (:clientId IS NOT NULL AND d.clientId = :clientId)) order by d.id desc "
            )
    Page<Allocations> findAllocations(@Param("name") String name,
                                      @Param("wareHouseId") Long wareHouseId,
                                      @Param("blockTypeId") Long blockTypeId,
                                      @Param("status")String status,
                                      @Param("clientId") Long clientId, Pageable pageable);

}
