package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.DropOffItem;
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
public interface DropOffItemRepository extends JpaRepository<DropOffItem, Long>, JpaSpecificationExecutor<DropOffItem> {

    @Query("SELECT d FROM DropOffItem d WHERE ((:dropOffId IS NULL) OR (:dropOffId IS NOT NULL AND d.dropOffId = :dropOffId))" +
            " AND ((:isActive IS NULL) OR (:isActive IS NOT NULL AND d.isActive = :isActive))")
    List<DropOffItem> findByDropOffIdAndIsActive(@Param("dropOffId") Long dropOffId, @Param("isActive") Boolean isActive);



    DropOffItem findByOrderItemIdAndDropOffId(Long orderItemId, Long dropOffId);

    @Query("SELECT ti from DropOffItem ti inner join OrderItem oi on ti.orderItemId = oi.id  where ((:orderId IS NULL) OR (oi.orderId = :orderId)) and ((:dropOffId IS NULL) OR(ti.dropOffId = :dropOffId))")
    List<DropOffItem> findByDropOffIdAndOrderId(@Param("dropOffId")Long dropOffId,
                                                @Param("orderId") Long orderId);

    List<DropOffItem> findByDropOffId(Long dropOffId);

    @Query("SELECT d FROM DropOffItem d WHERE ((:orderItemId IS NULL) OR (:orderItemId IS NOT NULL AND d.orderItemId = :orderItemId))" +
            " AND ((:dropOffId IS NULL) OR (:dropOffId IS NOT NULL AND d.dropOffId = :dropOffId))" +
            " AND ((:status IS NULL) OR (:status IS NOT NULL AND d.status like %:status%)) order by d.id desc")
    Page<DropOffItem> findDropOffItem(@Param("orderItemId") Long orderItemId,
                                      @Param("dropOffId") Long dropOffId,
                                      @Param("status") String status,
                                                      Pageable pageable);

    DropOffItem findDropOffItemByDropOffId(Long dropOffId);

    DropOffItem findByOrderItemIdAndStatus(Long orderItemId, String status);


    @Query("SELECT d from DropOffItem d inner join DropOff od on d.dropOffId = od.id  where ((:tripRequestId IS NULL) OR (:tripRequestId IS NOT NULL AND od.tripRequestId = :tripRequestId)) and ((:thirdPartyProductId IS NULL) OR(:thirdPartyProductId IS NOT NULL AND d.thirdPartyProductId = :thirdPartyProductId))")
    List<DropOffItem> findByTripRequestIdAndThirdPartyProductId(@Param("tripRequestId") Long tripRequestId,
                                                                @Param("thirdPartyProductId") Long thirdPartyProductId);


}
