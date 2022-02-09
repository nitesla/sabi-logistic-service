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

@SuppressWarnings("All")
@Repository
public interface DropOffRepository extends JpaRepository<DropOff, Long>, JpaSpecificationExecutor<DropOff> {


    List<DropOff> findByIsActiveAndTripRequestId(Boolean isActive, Long tripRequestId);

    DropOff findByTripRequestIdAndOrderId(Long tripRequestId, Long dropOffId);

    List<DropOff> findByTripRequestId(Long tripRequestId);

    Integer countByTripRequestId(Long ID);

    @Query("SELECT d FROM DropOff d WHERE ((:orderId IS NULL) OR (:orderId IS NOT NULL AND d.orderId = :orderId))" +
            " AND ((:tripRequestId IS NULL) OR (:tripRequestId IS NOT NULL AND d.tripRequestId = :tripRequestId)) order by d.id desc")
    Page<DropOff> findDropOff(@Param("orderId") Long orderId,
                              @Param("tripRequestId") Long tripRequestId,
                              Pageable pageable);

    List<DropOff> findByTripRequestIdAndPaidStatus(Long tripRequestId, String paidStatus);


}
