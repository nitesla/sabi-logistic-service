package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.DriverWallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverWalletRepository extends JpaRepository<DriverWallet, Long> {

    @Query("SELECT l FROM DriverWallet l WHERE ((:driverId IS NULL) OR (:driverId IS NOT NULL AND l.driverId = :driverId))" )
//            " AND ((:stateId IS NULL) OR (:stateId IS NOT NULL AND l.stateId = :stateId))")
    Page<DriverWallet> findDriverWallets(@Param("driverId") Long driverId,
                                Pageable pageable);
    List<DriverWallet>findAll();

    DriverWallet findByDriverId(Long driverId);

    List<DriverWallet> findDriverWalletById(Long id);

}
