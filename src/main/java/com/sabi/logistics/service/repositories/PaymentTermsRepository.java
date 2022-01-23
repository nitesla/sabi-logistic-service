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

    PaymentTerms findByPartnerAssetTypeIdAndCreatedBy (Long partnerAssetTypeId, Long createdBy);



    @Query("SELECT pa FROM PaymentTerms pa inner join PartnerAssetType pt on pa.partnerAssetTypeId = pt.id  WHERE ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND pt.partnerId = :partnerId))" +
            " AND ((:isActive IS NULL) OR (:isActive IS NOT NULL AND pa.isActive = :isActive))")
    List<PaymentTerms> findByPartnerIdAndIsActive(@Param("partnerId") Long partnerId, @Param("isActive") Boolean isActive);

    @Query("SELECT pa FROM PaymentTerms pa inner join PartnerAssetType pt on pa.partnerAssetTypeId = pt.id WHERE ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND pt.partnerId = :partnerId))" +
            " AND ((:days IS NULL) OR (:days IS NOT NULL AND pa.days = :days))" +
            " AND ((:partnerAssetTypeId IS NULL) OR (:partnerAssetTypeId IS NOT NULL AND pa.partnerAssetTypeId = :partnerAssetTypeId))")
    Page<PaymentTerms> findPaymentTerms(@Param("partnerAssetTypeId")Long partnerAssetTypeId,
                                        @Param("days")Integer days,
                                        @Param("partnerId") Long partnerId,
                                        Pageable pageable);
    }
