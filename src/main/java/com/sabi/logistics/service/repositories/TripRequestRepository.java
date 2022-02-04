package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.TripRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
            " AND ((:status IS NULL) OR (:status IS NOT NULL AND d.status like %:status%))")
    Integer countByPartnerIDAndStatus(@Param("partnerId") Long partnerId, @Param("status") String status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    TripRequest findByPartnerIdAndReferenceNo(Long partnerId, String referenceNo);

    @Query(value = "SELECT Count(d) FROM TripRequest d WHERE ((:driverId IS NULL) OR (:driverId IS NOT NULL AND d.driverId >= :driverId)) AND ((:status IS NULL) OR (:status IS NOT NULL AND  d.status <= :status))" +
            " AND ((:driverId IS NULL) OR (:driverId IS NOT NULL AND d.driverId = :driverId))" +
            " AND ((:status IS NULL) OR (:status IS NOT NULL AND d.status like %:status%))")
    Integer countByDriverIdAndStatus(@Param("driverId") Long partnerId, @Param("status") String status);



    @Query("SELECT t FROM TripRequest t ")
    List<TripRequest> listTrips();


    @Query("SELECT t FROM TripRequest t WHERE ((:partnerId IS NULL and :unassigned is null )OR (t.partnerId IS NULL and :unassigned = true ) OR (:partnerId IS NOT NULL AND t.partnerId = :partnerId))" +
            " AND ((:status IS NULL) OR (:status IS NOT NULL AND t.status like %:status%))" +
            " AND ((:referenceNo IS NULL) OR (:referenceNo IS NOT NULL AND t.referenceNo like %:referenceNo%))" +
            " AND ((:driverId IS NULL) OR (:driverId IS NOT NULL AND t.driverId = :driverId))" +
            " AND ((:driverAssistantId IS NULL) OR (:driverAssistantId IS NOT NULL AND t.driverAssistantId = :driverAssistantId))" +
            " AND ((:wareHouseId IS NULL) OR (:wareHouseId IS NOT NULL AND t.wareHouseId = :wareHouseId))" +
            " AND ((:wareHouseAddress IS NULL) OR (:wareHouseAddress IS NOT NULL AND t.wareHouseAddress like %:wareHouseAddress%))" +
            " AND ((:deliveryStatus IS NULL) OR (:deliveryStatus like '%all%' AND t.deliveryStatus NOT like '%pending%' AND t.deliveryStatus NOT like '%cancelled%') OR (:deliveryStatus IS NOT NULL AND t.deliveryStatus like %:deliveryStatus%))" +
            " AND ((:partnerAssetId IS NULL) OR (:partnerAssetId IS NOT NULL AND t.partnerAssetId = :partnerAssetId)) order by t.id desc")
    Page<TripRequest> findTripRequest(@Param("partnerId") Long partnerId,
                          @Param("status") String status,
                          @Param("referenceNo") String referenceNo,
                          @Param("driverId") Long driverId,
                          @Param("driverAssistantId") Long driverAssistantId,
                          @Param("wareHouseId") Long wareHouseId,
                          @Param("wareHouseAddress") String wareHouseAddress,
                          @Param("partnerAssetId") Long partnerAssetId,
                          @Param("unassigned") Boolean unassigned,
                          @Param("deliveryStatus") String deliveryStatus,
                          Pageable pageable);







    TripRequest findByReferenceNo(String referenceNo);

}
