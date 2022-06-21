package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.DropOffInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface DropOffInvoiceRepository extends JpaRepository<DropOffInvoice, Long>, JpaSpecificationExecutor<DropOffInvoice> {


    List<DropOffInvoice> findByIsActive(Boolean isActive);

    DropOffInvoice findByDropOffIdAndInvoiceId(Long dropOffId, Long invoiceId);

    List<DropOffInvoice> findByDropOffId(Long Id);

    @Query("SELECT t FROM DropOffInvoice t WHERE ((:dropOffId IS NULL) OR (:dropOffId IS NOT NULL AND t.dropOffId = :dropOffId))" +
            " AND ((:invoiceId IS NULL) OR (:invoiceId IS NOT NULL AND t.invoiceId = :invoiceId))" +
            " AND ((:status IS NULL) OR (:status IS NOT NULL AND t.status like %:status%)) order by t.id desc")
    Page<DropOffInvoice> findByDropOffInvoice(@Param("dropOffId") Long dropOffId,
                                                      @Param("invoiceId") Long invoiceId, @Param("status") String status,
                                                      Pageable pageable);


}
