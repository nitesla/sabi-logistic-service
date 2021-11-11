package com.sabi.logistics.service.repositories;



import com.sabi.logistics.core.models.TripRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TripRequestRepository extends JpaRepository<TripRequest, Long>, JpaSpecificationExecutor<TripRequest> {


    List<TripRequest> findByIsActive(Boolean isActive);

    TripRequest findByOrderItemIDAndPartnerID(Long orderItemId, Long partnerAssetId );


}
