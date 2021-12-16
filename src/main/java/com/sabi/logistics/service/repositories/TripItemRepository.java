package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.TripItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TripItemRepository extends JpaRepository<TripItem, Long>, JpaSpecificationExecutor<TripItem> {


    List<TripItem> findByIsActive(Boolean isActive);

    TripItem findByTripRequestIdAndThirdPartyProductId(Long tripRequestId, Long thirdPartyProductId);

    List<TripItem> findByTripRequestId(Long Id);


}
