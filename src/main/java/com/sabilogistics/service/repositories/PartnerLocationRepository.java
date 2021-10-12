package com.sabilogistics.service.repositories;

import com.sabilogisticscore.models.PartnerLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerLocationRepository extends JpaRepository<PartnerLocation, Long> {

    PartnerLocation findByPartnerId(Long partnerId);
    PartnerLocation findByCategoryIdId(Long categoryId);
    List<PartnerLocation> findByIsActive(Boolean isActive);

    @Query("SELECT c FROM PartnerLocation c WHERE ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND c.partnerId = :partnerId))" +
            " AND ((:categoryId IS NULL) OR (:categoryId IS NOT NULL AND c.categoryId = :categoryId))")
    Page<PartnerLocation> findPartnerLocation(@Param("partnerId") Long partnerId,
                                              @Param("categoryId") Long categoryId,
                                              Pageable pageable);
}
