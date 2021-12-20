package com.sabi.logistics.service.repositories;



import com.sabi.logistics.core.models.TripRequestResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TripRequestResponseRepository extends JpaRepository<TripRequestResponse, Long>, JpaSpecificationExecutor<TripRequestResponse> {


    List<TripRequestResponse> findByIsActive(Boolean isActive);

    TripRequestResponse findByTripRequestIdAndPartnerId(Long tripRequestId, Long partnerId);

    List<TripRequestResponse> findByTripRequestId(Long id);

    TripRequestResponse findTripRequestResponseByTripRequestId(Long tripRequestId);

}
