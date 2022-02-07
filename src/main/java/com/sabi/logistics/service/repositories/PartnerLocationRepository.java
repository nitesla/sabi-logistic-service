package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.PartnerLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerLocationRepository extends JpaRepository<PartnerLocation, Long> {

    PartnerLocation findPartnerLocationById(Long id);
    List<PartnerLocation> findByIsActive(Boolean isActive);

    @Query("SELECT l FROM PartnerLocation l WHERE ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND l.partnerId = :partnerId))" +
            " AND ((:isActive IS NULL) OR (:isActive IS NOT NULL AND l.isActive = :isActive))")
    List<PartnerLocation> findByPartnerIdAndIsActive(@Param("partnerId") Long partnerId, @Param("isActive") Boolean isActive);


    @Query("SELECT s FROM PartnerLocation s WHERE ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND s.partnerId = :partnerId))" +
//            " AND ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND s.partnerId = :partnerId))" +
            " AND ((:stateId IS NULL) OR (:stateId IS NOT NULL AND s.stateId = :stateId))"
    )
    Page<PartnerLocation> findPartnerLocation(
                                              @Param("partnerId") Long partnerId,
                                              @Param("stateId") Long stateId,
                                              Pageable pageable);
}
