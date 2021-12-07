package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.FulfilmentDashboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FulfilmentDashboardRepository extends JpaRepository<FulfilmentDashboard, Long> {

//    @Query("SELECT l FROM FulfilmentDashboard l WHERE ((:date IS NULL) OR (:date IS NOT NULL AND l.date = :date))"
////            " AND ((:createdDate IS NULL) OR (:createdDate IS NOT NULL AND l.createdDate >= :createdDate))"
////            " AND ((:productName IS NULL) OR (:productName IS NOT NULL AND c.productName = :productName))"
//
//    )
@Query(value = "SELECT d FROM FulfilmentDashboard d WHERE ((:startDate IS NULL) OR (:startDate IS NOT NULL AND d.date >= :startDate)) AND ((:endDate IS NULL) OR (:endDate IS NOT NULL AND  d.date <= :endDate))"
        )
    List<FulfilmentDashboard> findByDate(
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
}
