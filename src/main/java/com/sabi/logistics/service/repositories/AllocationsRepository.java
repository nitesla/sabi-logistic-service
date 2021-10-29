package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.AllocationHistory;
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

    @Query("SELECT d FROM Allocations d WHERE ((:name IS NULL) OR (:name IS NOT NULL AND d.name = :name))")
    Page<Allocations> findAllocations(@Param("name") String name, Pageable pageable);

}
