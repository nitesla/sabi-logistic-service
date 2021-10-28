package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.PartnerAssetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PartnerAssetTypeRepository extends JpaRepository<PartnerAssetType, Long> {

    List<PartnerAssetType> findByIsActive(Boolean isActive);
    PartnerAssetType findByAssetTypeId(Long name);

    @Query("SELECT s FROM PartnerAssetType s WHERE ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND s.partnerId = :partnerId))")
    Page<PartnerAssetType> findPartnerAssetType(Long partnerId, Pageable pageRequest);
}

