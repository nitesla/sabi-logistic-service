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

@Query(value = "SELECT d FROM FulfilmentDashboard d WHERE ((:startDate IS NULL) OR (:startDate IS NOT NULL AND d.date >= :startDate)) " +
        "AND ((:endDate IS NULL) OR (:endDate IS NOT NULL AND  d.date <= :endDate)) " +
        "AND ((:wareHouseId IS NULL) OR (:wareHouseId IS NOT NULL AND  d.wareHouseId = :wareHouseId)) " +
        "AND ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND  d.partnerId = :partnerId))"
        )


//@Query("SELECT c FROM FulfilmentDashboard c WHERE ((:startDate IS NULL) OR (:startDate IS NOT NULL AND c.date >= :startDate))" +
//        " AND ((:endDate IS NULL) OR (:endDate IS NOT NULL AND c.date = :endDate))" +
//        " AND ((:wareHouseId IS NULL) OR (:wareHouseId IS NOT NULL AND c.wareHouseId = :wareHouseId))" +
//        " AND ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND c.partnerId = :partnerId))"
//)
    List<FulfilmentDashboard> findFulfilmentDashboardInfo(
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate,
                                            @Param("wareHouseId") Long wareHouseId,
                                             @Param("partnerId") Long partnerId);
}
