package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {


    Order findByReferenceNo(String referenceNo);

    Order findOrderById(Long id);

    List<Order> findByIsActive(Boolean isActive);

    @Query("SELECT o FROM Order o WHERE ((:referenceNo IS NULL) OR (:referenceNo IS NOT NULL AND o.referenceNo like %:referenceNo%))" +
            " AND ((:deliveryStatus IS NULL) OR (:deliveryStatus IS NOT NULL AND o.deliveryStatus like %:deliveryStatus%))" +
            " AND ((:customerName IS NULL) OR (:customerName IS NOT NULL AND o.customerName like %:customerName%))" +
            " AND ((:customerPhone IS NULL) OR (:customerPhone IS NOT NULL AND o.customerPhone like %:customerPhone%))" +
            " AND ((:deliveryAddress IS NULL) OR (:deliveryAddress IS NOT NULL AND o.deliveryAddress like %:deliveryAddress%))" +
            " AND ((:barCode IS NULL) OR (:barCode IS NOT NULL AND o.barCode like %:barCode%))" +
            " AND ((:qrCode IS NULL) OR (:qrCode IS NOT NULL AND o.qrCode like %:qrCode%)) order by o.id desc")
    Page<Order> findOrder(@Param("referenceNo") String referenceNo,
                          @Param("deliveryStatus") String deliveryStatus,
                          @Param("customerName") String customerName,
                          @Param("customerPhone") String customerPhone,
                          @Param("deliveryAddress") String deliveryAddress,
                          @Param("barCode") String barCode,
                          @Param("qrCode") String qrCode,
                          Pageable pageable);


}