package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.TripRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface TripRequestRepository extends JpaRepository<TripRequest, Long>, JpaSpecificationExecutor<TripRequest> {


    List<TripRequest> findByIsActive(Boolean isActive);

    Integer countByPartnerIdAndDeliveryStatusAndIsActiveAndCreatedDateGreaterThanEqual(Long partnerId, String deliveryStatus, Boolean isActive, LocalDateTime date);

    List<TripRequest> findByCreatedDate(LocalDateTime date);

    Integer countByPartnerIdAndPartnerAssetId(Long partnerId, Long partnerAssetId);

//    List<TripRequest> findByPartnerIdAndIsActive(Long partnerId, Boolean isActive);

    @Query(value = "SELECT Count(d) FROM TripRequest d WHERE ((:startDate IS NULL) OR (:startDate IS NOT NULL AND d.createdDate >= :startDate)) AND ((:endDate IS NULL) OR (:endDate IS NOT NULL AND  d.createdDate <= :endDate))" +
            " AND ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND d.partnerId = :partnerId))" +
            " AND ((:status IS NULL) OR (:status IS NOT NULL AND d.status = :status))")
    Integer countByPartnerIDAndStatus(@Param("partnerId") Long partnerId, @Param("status") String status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);


}
