package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.PartnerBank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerBankRepository extends JpaRepository<PartnerBank, Long>, JpaSpecificationExecutor<PartnerBank> {

    PartnerBank findByPartnerIdAndAccountNumber (Long partnerId, String accountNumber);

    List<PartnerBank> findByIsActive(Boolean isActive);

    @Query("SELECT b FROM PartnerBank b WHERE ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND b.partnerId = :partnerId))" +
            " AND ((:isActive IS NULL) OR (:isActive IS NOT NULL AND b.isActive = :isActive))")
    List<PartnerBank> findByPartnerIdAndIsActive(@Param("partnerId") Long partnerId, @Param("isActive") Boolean isActive);

    @Query("SELECT b FROM PartnerBank b WHERE ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND b.partnerId = :partnerId))" +
            " AND ((:bankId IS NULL) OR (:bankId IS NOT NULL AND b.bankId = :bankId))" +
            " AND ((:accountNumber IS NULL) OR (:accountNumber IS NOT NULL AND b.accountNumber like %:accountNumber%))")
    Page<PartnerBank> findPartnerBanks(@Param("partnerId")Long partnerId,
                                       @Param("bankId")Long bankId,
                                       @Param("accountNumber")String accountNumber,
                                       Pageable pageable);
    @Modifying
    @Query(value = "UPDATE PartnerBank SET isDefault = 0")
    void updateIsDefault();
}
