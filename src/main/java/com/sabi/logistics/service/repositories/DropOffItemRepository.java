package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.DropOffItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface DropOffItemRepository extends JpaRepository<DropOffItem, Long>, JpaSpecificationExecutor<DropOffItem> {

    @Query("SELECT d FROM DropOffItem d WHERE ((:dropOffId IS NULL) OR (:dropOffId IS NOT NULL AND d.dropOffId = :dropOffId))" +
            " AND ((:isActive IS NULL) OR (:isActive IS NOT NULL AND d.isActive = :isActive))")
    List<DropOffItem> findByDropOffIdAndIsActive(@Param("dropOffId") Long dropOffId, @Param("isActive") Boolean isActive);



    DropOffItem findByOrderItemIdAndDropOffId(Long orderItemId, Long dropOffId);

    @Query("SELECT ti from DropOffItem ti inner join OrderItem oi on ti.orderItemId = oi.id  where ((:orderId IS NULL) OR (oi.orderId = :orderId)) and ((:dropOffId IS NULL) OR(ti.dropOffId = :dropOffId))")
    List<DropOffItem> findByDropOffIdAndOrderId(@Param("dropOffId")Long dropOffId,
                                                @Param("orderId") Long orderId);


}