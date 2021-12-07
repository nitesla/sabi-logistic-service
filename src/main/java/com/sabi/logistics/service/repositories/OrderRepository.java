package com.sabi.logistics.service.repositories;



import com.sabi.logistics.core.models.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {


    Order findByReferenceNo(String referenceNo);

    List<Order> findByIsActive(Boolean isActive);

    List<Order> findByDeliveryStatus(String status);

    Order findOrderById(Long id);

    @Query(value = "SELECT d FROM Order d WHERE ((:startDate IS NULL) OR (:startDate IS NOT NULL AND d.createdDate >= :startDate)) AND ((:endDate IS NULL) OR (:endDate IS NOT NULL AND  d.createdDate <= :endDate))"
    )
    List<Order> findOrderByCreatedDate(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT d FROM Order d WHERE ((:startDate IS NULL) OR (:startDate IS NOT NULL AND d.createdDate >= :startDate)) " +
            "AND ((:endDate IS NULL) OR (:endDate IS NOT NULL AND  d.createdDate <= :endDate))" +
            " AND ((:deliveryStatus IS NULL) OR (:deliveryStatus IS NOT NULL AND d.deliveryStatus = :deliveryStatus))"
    )
    List<Order> findOrderByCreatedDateAAndDeliveryStatus(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("deliveryStatus") String deliveryStatus);
    @Query(value = "SELECT d FROM Order d WHERE ((:startDate IS NULL) OR (:startDate IS NOT NULL AND d.createdDate >= :startDate)) AND ((:endDate IS NULL) OR (:endDate IS NOT NULL AND  d.createdDate <= :endDate))"
    )
    List<Order> findByDate(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}