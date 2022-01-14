package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.AllocationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AllocationHistoryRepository extends JpaRepository<AllocationHistory, Long> {

    AllocationHistory findAllocationHistoriesById(Long id);

    List<AllocationHistory> findByAllocationId (Long allocationId);


    List<AllocationHistory> findByIsActive(Boolean isActive);

    @Query("SELECT c FROM AllocationHistory c WHERE ((:allocationId IS NULL) OR (:allocationId IS NOT NULL AND c.allocationId = :allocationId))" +
            " AND ((:clientId IS NULL) OR (:clientId IS NOT NULL AND c.clientId = :clientId))" +
            " AND ((:amountPaid IS NULL) OR (:amountPaid IS NOT NULL AND c.amountPaid = :amountPaid))" +
            " AND ((:totalAmount IS NULL) OR (:totalAmount  IS NOT NULL AND c.totalAmount = :totalAmount))" +
            " AND ((:balance IS NULL) OR (:balance  IS NOT NULL AND c.balance = :balance)) order by c.id desc"


    )
    Page<AllocationHistory> findAllocationHistory(@Param("allocationId") Long allocationId,
                                                  @Param("clientId") Long clientId,
                                                  @Param("amountPaid") BigDecimal amountPaid,
                                                  @Param("totalAmount") BigDecimal totalAmount,
                                                  @Param("balance") BigDecimal balance,
                                                  Pageable pageable);
}
