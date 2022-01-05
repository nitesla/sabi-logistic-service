package com.sabi.logistics.service.repositories;



import com.sabi.logistics.core.models.DashboardSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface DashboardSummaryRepository extends JpaRepository<DashboardSummary, Long> {

    @Query(value = "SELECT d FROM DashboardSummary d WHERE ((:startDate IS NULL) OR (:startDate IS NOT NULL AND d.date >= :startDate)) AND ((:endDate IS NULL) OR (:endDate IS NOT NULL AND  d.date <= :endDate))" +
    " AND ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND d.partnerId = :partnerId))")
    List<DashboardSummary> getAllBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("partnerId")Long partnerId);


    DashboardSummary findByPartnerIdAndReferenceNo(Long partnerId, String referenceNo);




    @Query("select d.partnerId,COUNT(d.deliveryStatus) AS completedTrips,SUM(d.earnings) AS totalEarnings,d.date from DashboardSummary d" +
            " where d.deliveryStatus='Completed' AND ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND d.partnerId = :partnerId)) " +
            "AND ((:startDate IS NULL) OR (:startDate IS NOT NULL AND d.date >= :startDate)) " +
            "AND ((:endDate IS NULL) OR (:endDate IS NOT NULL AND  d.date <= :endDate)) GROUP BY d.partnerId,d.date")
    List<Object[]> GetTotalTripsAndTotalEarnings (@Param("partnerId")Long partnerId,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);



    @Query("select d.partnerId,COUNT(d.deliveryStatus) AS outStandingTrips,SUM(d.earnings) AS outstandingEarnings,d.date from DashboardSummary d" +
            " where d.deliveryStatus='Pending' OR d.deliveryStatus='Cancelled' OR d.deliveryStatus='Ongoing' AND ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND d.partnerId = :partnerId)) " +
            "AND ((:startDate IS NULL) OR (:startDate IS NOT NULL AND d.date >= :startDate)) " +
            "AND ((:endDate IS NULL) OR (:endDate IS NOT NULL AND  d.date <= :endDate)) GROUP BY d.partnerId,d.date")
    List<Object[]> GetOutStandingTripsAndOutStandEarnings (@Param("partnerId")Long partnerId,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);







}
