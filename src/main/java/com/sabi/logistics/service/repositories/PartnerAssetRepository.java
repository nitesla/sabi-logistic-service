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

@Query("SELECT pa from PartnerAsset pa inner join PartnerAssetType pt on pa.partnerAssetTypeId = pt.id  where ((:partnerId IS NULL) OR (pt.partnerId = :partnerId)) and ((:isActive IS NULL) OR(pa.isActive = :isActive))")
List<PartnerAsset> findByIsActiveAndId(@Param("partnerId") Long partnerId,
                                           @Param("isActive") Boolean isActive);

    @Query("SELECT pa FROM PartnerAsset pa inner join PartnerAssetType pt on pa.partnerAssetTypeId = pt.id WHERE ((:partnerId IS NULL) OR (pt.partnerId = :partnerId)) and ((:name IS NULL) OR (:name IS NOT NULL AND pa.name = :name))" +
            " AND ((:brandId IS NULL) OR (:brandId IS NOT NULL AND pa.brandId = :brandId))" +
            " AND ((:status IS NULL) OR (:status IS NOT NULL AND pa.status = :status))" +
            " AND ((:driverId IS NULL) OR (:driverId IS NOT NULL AND pa.driverId = :driverId))" +
            " AND ((:partnerAssetTypeId IS NULL) OR (:partnerAssetTypeId IS NOT NULL AND pa.partnerAssetTypeId = :partnerAssetTypeId))" +
            " AND ((:isActive IS NULL) OR (:isActive IS NOT NULL AND pa.isActive = :isActive))")
    Page<PartnerAsset> findPartnerAsset(@Param("name") String name,
                                        @Param("brandId") Long brandId,
                                        @Param("status") String status,
                                        @Param("driverId") Long driverId,
                                        @Param("partnerId") Long partnerId,
                                        @Param("partnerAssetTypeId") Long partnerAssetTypeId,
                                        @Param("isActive") Boolean isActive,
                                        Pageable pageRequest);

}
