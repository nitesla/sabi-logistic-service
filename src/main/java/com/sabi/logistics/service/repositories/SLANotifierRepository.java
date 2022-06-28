package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.enums.SlaName;
import com.sabi.logistics.core.models.SLANotifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SLANotifierRepository extends JpaRepository<SLANotifier, Long> {

    public List<SLANotifier> findByName(String name);

    public List<SLANotifier> findByIsActive(Boolean isActive);

    public List<SLANotifier> findByEmail(String email);

    @Query("SELECT sln FROM SLANotifier sln INNER JOIN SLA sla ON sla.id = sln.slaId " +
            "WHERE (((:slaName IS NULL) OR ((:slaName IS NOT NULL) AND (sla.slaName = :slaName)))" +
            "AND ((:name IS NULL) OR ((:name IS NOT NULL) AND (sln.name = :name))))" +
            "AND ((:email IS NULL) OR ((:email IS NOT NULL) AND (sln.email = :email)))" +
            "AND ((:isActive IS NULL) OR ((:isActive IS NOT NULL) AND (sln.isActive = :isActive))) ORDER BY sln.id DESC ")
    public Page<SLANotifier> searchAll(SlaName slaName, String name, String email, Boolean isActive, Pageable pageable);
}
