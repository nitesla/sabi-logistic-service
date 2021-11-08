package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.Driver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    List<Driver> findByIsActive(Boolean isActive);

    List<Driver> findByPartnerIdAndIsActive(Long partnerId, Boolean isActive);

    Driver findByName (String name);

    @Query("SELECT d FROM Driver d WHERE ((:name IS NULL) OR (:name IS NOT NULL AND d.name = :name))" +
            " AND ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND d.partnerId = :partnerId))")
    Page<Driver> findDrivers(@Param("name") String name, @Param("partnerId") Long partnerId, Pageable pageable);
}
