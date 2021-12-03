package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.DashboardSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface DashboardSummaryRepository extends JpaRepository<DashboardSummary, Long> {




}
