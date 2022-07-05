package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.enums.PaymentChannel;
import com.sabi.logistics.core.models.DropOffInvoicePayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DropOffInvoicePaymentRepository extends JpaRepository<DropOffInvoicePayment, Long> {

    public DropOffInvoicePayment findByDropOffInvoiceIdAndInvoicePaymentId(Long dropOffInvoiceId, Long invoicePaymentId);

    public List<DropOffInvoicePayment> findByIsActive(Boolean isActive);

    @Query("SELECT dip FROM DropOffInvoicePayment dip INNER JOIN DropOffInvoice di ON dip.dropOffInvoiceId= di.id " +
            "INNER JOIN Invoice  i ON di.invoiceId = i.id INNER JOIN  InvoicePayment ip ON dip.invoicePaymentId = ip.id " +
            "INNER JOIN Invoice ipInvoice ON ip.invoiceId = ipInvoice.id WHERE " +
            "(((:customerName IS NULL) OR ((:customerName IS NOT NULL) AND (i.customerName like %:customerName%)))" +
            "AND ((:invoiceReferenceNo IS NULL) OR ((:invoiceReferenceNo IS NOT NULL) AND (i.referenceNo like %:invoiceReferenceNo%)))" +
            "AND ((:customerPhone IS NULL) OR ((:customerPhone IS NOT NULL) AND (i.customerPhone like %:customerPhone%)))" +
            "AND ((:paymentChannel IS NULL) OR ((:paymentChannel IS NOT NULL) AND (ip.paymentChannel =:paymentChannel)))" +
            "AND ((:deliveryStatus IS NULL) OR ((:deliveryStatus IS NOT NULL) AND (i.deliveryStatus like %:deliveryStatus%)))" +
            "AND ((:paymentReference IS NULL) OR ((:paymentReference IS NOT NULL) AND (ip.paymentReference like %:paymentReference%)))" +
            "AND ((:isActive IS NULL) OR ((:isActive IS NOT NULL) AND (dip.isActive = :isActive)))" +
            ") ORDER BY dip.id DESC ")
    public Page<DropOffInvoicePayment> searchAll(
            String invoiceReferenceNo, PaymentChannel paymentChannel,
            String deliveryStatus,String paymentReference,
            String customerName,
            String customerPhone, Boolean isActive, Pageable pageable);
}
