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

//    DriverAsset findByName (String name);

    List<DriverAsset> findByIsActive(Boolean isActive);
    DriverAsset findByDriverIdAndPartnerAssetId(Long driverId, Long partnerAssestId);
    DriverAsset findByPartnerAssetIdAndId(Long partnerAssetId,Long id);
    DriverAsset findByPartnerAssetId(Long partnerAssetId);

    @Query("SELECT d FROM DriverAsset d WHERE ((:driverId IS NULL) OR (:driverId IS NOT NULL AND d.driverId = :driverId))" +
            " AND ((:partnerAssetId IS NULL) OR (:partnerAssetId IS NOT NULL AND d.partnerAssetId = :partnerAssetId)) order by d.id desc"
    )
    Page<DriverAsset> findDriverAssets(@Param("driverId") Long driverId,
                                       @Param("partnerAssetId") Long partnerAssetId,
                                       Pageable pageable);
}
