package com.sabilogistics.service.repositories;

import com.sabilogisticscore.models.AssetTypeProperties;
import com.sabilogisticscore.models.PartnerProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerPropertiesRepository extends JpaRepository<PartnerProperties, Long> {


    PartnerProperties findByName(String name);

    PartnerProperties findByPhone(String phone);

    List<PartnerProperties> findByIsActive(Boolean isActive);

    @Query("SELECT p FROM PartnerProperties p WHERE ((:name IS NULL) OR (:name IS NOT NULL AND p.name = :name))")
    Page<PartnerProperties> findPartnersProperties(@Param("name") String name, Pageable pageable);

}
