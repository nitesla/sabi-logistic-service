package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.AllocationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AllocationHistoryRepository extends JpaRepository<AllocationHistory, Long> {

//    AllocationHistory findByName(String name);

    List<AllocationHistory> findByIsActive(Boolean isActive);

    @Query("SELECT c FROM AllocationHistory c WHERE ((:allocationId IS NULL) OR (:allocationId IS NOT NULL AND c.allocationId = :allocationId))" +
            " AND ((:startDate IS NULL) OR (:startDate IS NOT NULL AND c.startDate = :startDate))"
//            " AND ((:height IS NULL) OR (:height IS NOT NULL AND c.height = :height))" +
//            " AND ((:price >= 0.0) OR (:price  AND c.price = :price))" )
//            " AND ((:isActive IS NULL) OR (:isActive IS NOT NULL AND c.isActive = :isActive))
 )
    Page<AllocationHistory> findAllocationHistory(@Param("allocationId") Long allocationId,
                                                  @Param("startDate") LocalDateTime startDate, Pageable pageable);
}
