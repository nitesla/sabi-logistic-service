package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {


    Invoice findByReferenceNo(String referenceNo);

    Invoice findInvoiceById(Long id);

    Invoice findByInvoiceNumber(String InvoiceNumber);

    List<Invoice> findByIsActive(Boolean isActive);

    @Query("SELECT MAX(o.id) FROM Invoice o ")
    Long getLastInvoice();

    @Query("SELECT o FROM Invoice o INNER JOIN InvoiceItem it ON o.id = it.invoiceId WHERE ((:referenceNo IS NULL) OR (:referenceNo IS NOT NULL AND o.referenceNo like %:referenceNo%))" +
            " AND ((:deliveryStatus IS NULL) OR (:deliveryStatus IS NOT NULL AND o.deliveryStatus like %:deliveryStatus%))" +
            " AND ((:customerName IS NULL) OR (:customerName IS NOT NULL AND o.customerName like %:customerName%))" +
            " AND ((:customerPhone IS NULL) OR (:customerPhone IS NOT NULL AND o.customerPhone like %:customerPhone%))" +
            " AND ((:deliveryAddress IS NULL) OR (:deliveryAddress IS NOT NULL AND o.deliveryAddress like %:deliveryAddress%))" +
            " AND ((:barCode IS NULL) OR (:barCode IS NOT NULL AND o.barCode like %:barCode%))" +
            " AND ((:wareHouseId IS NULL) OR (:wareHouseId IS NOT NULL AND it.wareHouseId =:wareHouseId))" +
            " AND ((:qrCode IS NULL) OR (:qrCode IS NOT NULL AND o.qrCode like %:qrCode%)) ORDER BY o.id desc")
    Page<Invoice> findInvoice(@Param("referenceNo") String referenceNo,
                          @Param("deliveryStatus") String deliveryStatus,
                          @Param("customerName") String customerName,
                          @Param("customerPhone") String customerPhone,
                          @Param("deliveryAddress") String deliveryAddress,
                          Long wareHouseId ,
                          @Param("barCode") String barCode,
                          @Param("qrCode") String qrCode,
                          Pageable pageable);


}