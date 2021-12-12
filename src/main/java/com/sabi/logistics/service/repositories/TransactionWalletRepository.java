package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionWalletRepository extends JpaRepository<WalletTransaction, Long> {

    @Query("SELECT l FROM WalletTransaction l WHERE ((:driverId IS NULL) OR (:driverId IS NOT NULL AND l.driverWalletId = :driverId))" +
            " AND ((:dropOffId IS NULL) OR (:dropOffId IS NOT NULL AND l.dropOffId = :dropOffId))")
    Page<WalletTransaction> findWalletTransactions(@Param("driverId") Long driverId,
                                                   @Param("dropOffId") Long dropOffId,
                                              Pageable pageable);
    List<WalletTransaction> findAll();

    List<WalletTransaction> findByDriverWalletId(Long driverWalletId);
}
