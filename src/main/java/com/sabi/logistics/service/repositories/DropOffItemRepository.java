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
            " AND ((:isActive IS NULL) OR (:isActive IS NOT NULL AND d.isActive = :isActive)) order by d.id desc ")
    List<DropOffItem> findByDropOffIdAndIsActive(@Param("dropOffId") Long dropOffId, @Param("isActive") Boolean isActive);



    DropOffItem findByInvoiceItemIdAndDropOffId(Long invoiceItemId, Long dropOffId);

    @Query("SELECT ti from DropOffItem ti inner join InvoiceItem oi on ti.invoiceItemId = oi.id  where ((:invoiceId IS NULL) OR (oi.invoiceId = :invoiceId)) and ((:dropOffId IS NULL) OR(ti.dropOffId = :dropOffId)) order by ti.id desc ")
    List<DropOffItem> findByDropOffIdAndInvoiceId(@Param("dropOffId")Long dropOffId,
                                                @Param("invoiceId") Long invoiceId);

    List<DropOffItem> findByDropOffId(Long dropOffId);

    @Query("SELECT d FROM DropOffItem d WHERE ((:invoiceItemId IS NULL) OR (:invoiceItemId IS NOT NULL AND d.invoiceItemId = :invoiceItemId))" +
            " AND ((:dropOffId IS NULL) OR (:dropOffId IS NOT NULL AND d.dropOffId = :dropOffId))" +
            " AND ((:status IS NULL) OR (:status IS NOT NULL AND d.status like %:status%)) order by d.id desc")
    Page<DropOffItem> findDropOffItem(@Param("invoiceItemId") Long invoiceItemId,
                                      @Param("dropOffId") Long dropOffId,
                                      @Param("status") String status,
                                                      Pageable pageable);

    DropOffItem findDropOffItemByDropOffId(Long dropOffId);

    DropOffItem findByInvoiceItemIdAndStatus(Long invoiceItemId, String status);


    @Query("SELECT d from DropOffItem d inner join DropOff od on d.dropOffId = od.id  where ((:tripRequestId IS NULL) OR (:tripRequestId IS NOT NULL AND od.tripRequestId = :tripRequestId)) and ((:thirdPartyProductId IS NULL) OR(:thirdPartyProductId IS NOT NULL AND d.thirdPartyProductId = :thirdPartyProductId)) order by d.id desc ")
    List<DropOffItem> findByTripRequestIdAndThirdPartyProductId(@Param("tripRequestId") Long tripRequestId,
                                                                @Param("thirdPartyProductId") String thirdPartyProductId);


}
