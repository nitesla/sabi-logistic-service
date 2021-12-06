package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.TripRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface TripRequestRepository extends JpaRepository<TripRequest, Long>, JpaSpecificationExecutor<TripRequest> {


    List<TripRequest> findByIsActive(Boolean isActive);

    TripRequest findByPartnerID(Long partnerId );

    Integer countByPartnerIDAndDeliveryStatusAndIsActiveAndCreatedDateGreaterThanEqual(Long partnerId, String deliveryStatus, Boolean isActive, LocalDateTime date);

    List<TripRequest> findByCreatedDate(LocalDateTime date);

    Integer countByPartnerIDAndPartnerAssetID(Long partnerId, Long partnerAssetId);

    List<TripRequest> findByPartnerIDAndIsActive(Long partnerId, Boolean isActive);


}
