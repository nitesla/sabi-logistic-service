package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.PartnerAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerAssetRepository extends JpaRepository<PartnerAsset, Long> {
    PartnerAsset findByPlateNo(String plateNo);

    List<PartnerAsset> findByIsActive(Boolean isActive);

    @Query("SELECT c FROM PartnerAsset c WHERE ((:name IS NULL) OR (:name IS NOT NULL AND c.name = :name))")
    Page<PartnerAsset> findPartnerAsset(String name, Pageable pageRequest);
}
