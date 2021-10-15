package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.Driver;
import com.sabi.logistics.core.models.DriverAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverAssetRepository extends JpaRepository<DriverAsset, Long> {

    DriverAsset findByName (String name);

    List<DriverAsset> findByIsActive(Boolean isActive);

    @Query("SELECT d FROM DriverAsset d WHERE ((:name IS NULL) OR (:name IS NOT NULL AND d.name = :name))")
    Page<DriverAsset> findDriverAssets(@Param("name") String name, Pageable pageable);
}
