package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.DropOff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@SuppressWarnings("All")
@Repository
public interface DropOffRepository extends JpaRepository<DropOff, Long>, JpaSpecificationExecutor<DropOff> {


    List<DropOff> findByIsActiveAndTripRequestId(Boolean isActive, Long tripRequestId);

    DropOff findByTripRequestIdAndOrderId(Long tripRequestId, Long dropOffId);

    List<DropOff> findByTripRequestId(Long tripRequestId);

    Integer countByTripRequestId(Long ID);


}
