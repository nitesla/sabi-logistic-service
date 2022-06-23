package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long>, JpaSpecificationExecutor<OrderItem> {


    OrderItem findByThirdPartyProductId(String thirdPartyProductId);

    List<OrderItem> findByIsActive(Boolean isActive);

    List<OrderItem> findByOrderId(Long orderId);

    OrderItem findOrderItemById(Long Id);

    List<OrderItem> findByOrderIdAndThirdPartyProductId(Long orderId, String thirdPartyProductId);

    @Query("SELECT oi FROM OrderItem oi INNER JOIN Order o ON o.id = oi.orderId " +
            "WHERE (((:wareHouseId IS NULL ) OR (:wareHouseId IS NOT NULL AND oi.wareHouseId = :wareHouseId))" +
            "AND ((:deliveryStatus IS NULL ) OR (:deliveryStatus IS NOT NULL AND oi.deliveryStatus = :deliveryStatus))" +
            "AND ((:hasInventory IS NULL ) OR (:hasInventory IS NOT NULL AND oi.inventoryId >= 0))" +
            "AND ((:productName IS NULL ) OR (:productName IS NOT NULL AND oi.productName = :productName))" +
            "AND ((:qty IS NULL ) OR (:qty IS NOT NULL AND oi.qty = :qty))" +
            "AND ((:startDate IS NULL ) OR (:startDate IS NOT NULL AND oi.createdDate >= :startDate))" +
            "AND ((:endDate IS NULL ) OR (:endDate IS NOT NULL AND oi.createdDate <= :endDate))" +
            "AND ((:customerName IS NULL ) OR (:customerName IS NOT NULL AND o.customerName LIKE %:customerName%))) order by oi.id desc ")
    public Page<OrderItem> searchOrderItems(Long wareHouseId, String deliveryStatus, Boolean hasInventory,
                                            String productName, Integer qty, LocalDateTime startDate, LocalDateTime endDate, String customerName, Pageable pageable);

    OrderItem findByPaymentReference(String paymentReference);

}
