package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.TripItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TripItemRepository extends JpaRepository<TripItem, Long>, JpaSpecificationExecutor<TripItem> {


    List<TripItem> findByIsActive(Boolean isActive);

    TripItem findByOrderItemIDAndTripRequestID(Long orderItemID, Long tripRequestID);

    List<TripItem> findByTripRequestID(Long ID);

    Integer countTripItemByTripRequestID(Long ID);

    @Query("SELECT ti from TripItem ti inner join OrderItem oi on ti.orderItemID = oi.id  where ((:orderID IS NULL) OR (oi.orderID = :orderID)) and ((:tripRequestID IS NULL) OR(ti.tripRequestID = :tripRequestID))")
    List<TripItem> findByTripRequestIDAndOrderID(@Param("tripRequestID")Long tripRequestID,
                                                 @Param("orderID") Long orderID);


}
