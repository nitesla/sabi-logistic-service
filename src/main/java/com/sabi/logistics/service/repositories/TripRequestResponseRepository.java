package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.TripRequestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TripRequestResponseRepository extends JpaRepository<TripRequestResponse, Long>, JpaSpecificationExecutor<TripRequestResponse> {


    List<TripRequestResponse> findByIsActive(Boolean isActive);

    TripRequestResponse findByTripRequestIdAndPartnerId(Long tripRequestId, Long partnerId);

    List<TripRequestResponse> findByTripRequestId(Long id);

    TripRequestResponse findTripRequestResponseByTripRequestId(Long tripRequestId);

    @Query("SELECT t FROM TripRequestResponse t WHERE ((:tripRequestId IS NULL) OR (:tripRequestId IS NOT NULL AND t.tripRequestId = :tripRequestId))" +
            " AND ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND t.partnerId = :partnerId))" +
            " AND ((:status IS NULL) OR (:status IS NOT NULL AND t.status like %:status%)) order by t.id desc")
    Page<TripRequestResponse> findTripRequestResponse(@Param("tripRequestId") Long tripRequestId,
                           @Param("partnerId") Long partnerId, @Param("status") String status,
                           Pageable pageable);
}
