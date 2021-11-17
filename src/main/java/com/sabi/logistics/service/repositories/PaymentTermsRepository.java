package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.PaymentTerms;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentTermsRepository extends JpaRepository<PaymentTerms, Long>, JpaSpecificationExecutor<PaymentTerms> {

    PaymentTerms findByPartnerAssetTypeIdAndDays (Long partnerAssetTypeId, Integer days);

    List<PaymentTerms> findByIsActive(Boolean isActive);

    @Query("SELECT b FROM PaymentTerms b WHERE ((:partnerAssetTypeId IS NULL) OR (:partnerAssetTypeId IS NOT NULL AND b.partnerAssetTypeId = :partnerAssetTypeId))" +
            " AND ((:days IS NULL) OR (:days IS NOT NULL AND b.days = :days))")
    Page<PaymentTerms> findPaymentTerms(@Param("partnerAssetTypeId")Long partnerAssetTypeId,
                                        @Param("days")Integer days,
                                        Pageable pageable);
    }
