package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.enums.SlaName;
import com.sabi.logistics.core.models.SLA;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SLARepository extends JpaRepository<SLA, Long> {

    public List<SLA> findByIsActive(Boolean isActive);

    public SLA findBySlaName(SlaName slaName);

    @Query("SELECT sla FROM SLA sla WHERE " +
            "(((:slaDuration IS NULL ) OR ((:slaDuration IS NOT NULL) AND (sla.slaDuration =:slaDuration)))" +
            "AND ((:slaName IS NULL ) OR ((:slaName IS NOT NULL) AND (sla.slaName  =:slaName)))" +
            "AND ((:triggerDuration IS NULL ) OR ((:triggerDuration IS NOT NULL) AND (sla.triggerDuration =:triggerDuration)))" +
            "AND ((:isActive IS NULL ) OR ((:isActive IS NOT NULL) AND (sla.isActive =:isActive)))) ORDER BY sla.id DESC ")
    public Page<SLA> findAllBySlaDurationAndSlaNameAndTriggerDurationAndIsActive(Long slaDuration,SlaName slaName, Long triggerDuration, Boolean isActive, Pageable pageable);



}
