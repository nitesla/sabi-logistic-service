package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.PartnerAssetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerAssetTypeRepository extends JpaRepository<PartnerAssetType, Long> {

    List<PartnerAssetType> findByIsActive(Boolean isActive);
    PartnerAssetType findByAssetTypeId(Long name);

    PartnerAssetType findPartnerAssetTypeById(Long Id);

//    PartnerAssetType findByPartnerName(String partnerName);

    @Query("SELECT s FROM PartnerAssetType s WHERE ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND s.partnerId = :partnerId))" +
            " AND ((:assetTypeId IS NULL) OR (:assetTypeId IS NOT NULL AND s.assetTypeId = :assetTypeId))")
    Page<PartnerAssetType> findPartnerAssetType(Long partnerId, Long assetTypeId, Pageable pageRequest);
}

