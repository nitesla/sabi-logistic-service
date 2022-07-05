package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.enums.PaymentChannel;
import com.sabi.logistics.core.models.InvoicePayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment,Long> {

    public InvoicePayment findByPaymentReferenceAndInvoiceId(String paymentReference, Long invoiceId);

    public InvoicePayment findByPaymentReference(String paymentReference);

    public List<InvoicePayment> findByInvoiceId(Long invoiceId);

    public List<InvoicePayment> findByIsActive(Boolean isActive);


    @Query(" SELECT ip FROM InvoicePayment ip INNER JOIN Invoice i ON i.id = ip.invoiceId " +
            "WHERE (((:paymentChannel IS NULL ) OR (:paymentChannel IS NOT NULL AND (ip.paymentChannel = :paymentChannel)))" +
            "AND ((:totalAmount IS NULL ) OR (:totalAmount IS NOT NULL AND (ip.totalAmount = :totalAmount)))" +
            "AND ((:amountCollected IS NULL ) OR (:amountCollected IS NOT NULL AND (ip.amountCollected = :amountCollected)))" +
            "AND ((:balanceBefore IS NULL ) OR (:balanceBefore IS NOT NULL AND (ip.balanceBefore = :balanceBefore)))" +
            "AND ((:balanceAfter IS NULL ) OR (:balanceAfter IS NOT NULL AND (ip.balanceAfter = :balanceAfter)))" +
            "AND ((:invoiceId IS NULL ) OR (:invoiceId IS NOT NULL AND (i.id = :invoiceId)))" +
            "AND ((:balanceBefore IS NULL ) OR (:balanceBefore IS NOT NULL AND (ip.balanceBefore = :balanceBefore)))" +
            "AND ((:paymentReference IS NULL ) OR (:paymentReference IS NOT NULL AND (ip.paymentReference = :paymentReference)))" +
            "AND ((:isActive IS NULL ) OR (:isActive IS NOT NULL AND (ip.isActive = :isActive)))) ORDER BY ip.id DESC ")
    public Page<InvoicePayment> searchInvoicePayments(String paymentChannel,BigDecimal totalAmount, BigDecimal amountCollected, BigDecimal balanceBefore, BigDecimal balanceAfter,
                                                      Long invoiceId, String paymentReference, Boolean isActive, Pageable pageable);
}
