package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.PartnerAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerAssetRepository extends JpaRepository<PartnerAsset, Long> {
    PartnerAsset findByPlateNo(String plateNo);

    List<PartnerAsset> findByIsActive(Boolean isActive);

    @Query("SELECT c FROM PartnerAsset c WHERE ((:name IS NULL) OR (:name IS NOT NULL AND c.name = :name))" +
            " AND ((:brandId IS NULL) OR (:brandId IS NOT NULL AND c.brandId = :brandId))" +
            " AND ((:status IS NULL) OR (:status IS NOT NULL AND c.status = :status))" +
            " AND ((:driverId IS NULL) OR (:driverId IS NOT NULL AND c.driverId = :driverId))" +
            " AND ((:partnerAssetTypeId IS NULL) OR (:partnerAssetTypeId IS NOT NULL AND c.partnerAssetTypeId = :partnerAssetTypeId))")
    Page<PartnerAsset> findPartnerAsset(@Param("name") String name,
                                        @Param("brandId") Long brandId,
                                        @Param("status") String status,
                                        @Param("driverId") Long driverId,
                                        @Param("partnerAssetTypeId") Long partnerAssetTypeId, Pageable pageRequest);
}
