package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.DropOff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("All")
@Repository
public interface DropOffRepository extends JpaRepository<DropOff, Long>, JpaSpecificationExecutor<DropOff> {


    List<DropOff> findByIsActiveAndTripRequestId(Boolean isActive, Long tripRequestId);

    DropOff findByTripRequestIdAndOrderId(Long tripRequestId, Long dropOffId);

    Optional<DropOff> findByIdAndDeliveryOverrideCodeHash(Long id, String deliveryOverrideCodeHash);

    List<DropOff> findByTripRequestId(Long tripRequestId);

    Integer countByTripRequestId(Long ID);

    @Query("SELECT d FROM DropOff d WHERE ((:orderId IS NULL) OR (:orderId IS NOT NULL AND d.orderId = :orderId))" +
            " AND ((:tripRequestId IS NULL) OR (:tripRequestId IS NOT NULL AND d.tripRequestId = :tripRequestId)) order by d.id desc")
    Page<DropOff> findDropOff(@Param("orderId") Long orderId,
                              @Param("tripRequestId") Long tripRequestId,
                              Pageable pageable);

    List<DropOff> findByTripRequestIdAndPaidStatus(Long tripRequestId, String paidStatus);
    List<DropOff> findByTripRequestIdAndReturnStatus(Long tripRequestId, String retrunedStatus);
    List<DropOff>findByTripRequestIdAndPaidStatusAndReturnStatus(Long tripRequestId, String paidStatus,String returnedStatus);


    @Query("SELECT do FROM DropOff do INNER JOIN TripRequest tr ON do.tripRequestId = tr.id INNER JOIN  Driver dr ON dr.id= tr.driverId WHERE (do.returnStatus = :returnedStatus AND  dr.userId = :driverUserId) order by tr.id desc ")
    public List<DropOff> getAllDropOffsOfADriver(Long driverUserId, String returnedStatus);


}
