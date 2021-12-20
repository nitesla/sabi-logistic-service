package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.DashboardSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface DashboardSummaryRepository extends JpaRepository<DashboardSummary, Long> {

//    @Query(value = "SELECT d FROM DashboardSummary d WHERE ((:startDate IS NULL) OR (:startDate IS NOT NULL AND d.date >= :startDate)) AND ((:endDate IS NULL) OR (:endDate IS NOT NULL AND  d.date <= :endDate))" +
//    " AND ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND d.partnerId = :partnerId))")
//    List<DashboardSummary> getAllBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("partnerId")Long partnerId);





}
