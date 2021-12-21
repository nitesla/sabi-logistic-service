package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.PricingConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PricingConfigurationRepository extends JpaRepository<PricingConfiguration, Long>, JpaSpecificationExecutor<PricingConfiguration> {

    List<PricingConfiguration> findByIsActive(Boolean isActive);
}
