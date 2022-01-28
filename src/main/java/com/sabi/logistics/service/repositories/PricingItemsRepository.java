package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.PricingItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PricingItemsRepository extends JpaRepository<PricingItems, Long>, JpaSpecificationExecutor<PricingItems> {
    PricingItems findByPartnerAssetTypeId(Long partnerAssetTypeId);

    List<PricingItems> findByIsActive(Boolean isActive);
}
