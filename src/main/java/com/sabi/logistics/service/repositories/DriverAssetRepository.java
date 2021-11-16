package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.DriverAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverAssetRepository extends JpaRepository<DriverAsset, Long> {

    DriverAsset findByName (String name);

    List<DriverAsset> findByIsActive(Boolean isActive);

    @Query("SELECT d FROM DriverAsset d WHERE ((:name IS NULL) OR (:name IS NOT NULL AND d.name = :name))" +
            " AND ((:driverId IS NULL) OR (:driverId IS NOT NULL AND d.driverId = :driverId))" +
            " AND ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND d.partnerId = :partnerId))" +
            " AND ((:partnerAssetTypeId IS NULL) OR (:partnerAssetTypeId IS NOT NULL AND d.partnerAssetTypeId = :partnerAssetTypeId))"
    )
    Page<DriverAsset> findDriverAssets(@Param("name") String name, Long driverId, Long partnerId, Long partnerAssetTypeId, Pageable pageable);
}
