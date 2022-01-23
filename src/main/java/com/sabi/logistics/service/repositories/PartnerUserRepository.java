package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.PartnerUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerUserRepository extends JpaRepository<PartnerUser, Long> {

    PartnerUser findByUserId(Long userId);




    @Query("SELECT p FROM PartnerUser p WHERE ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND p.partnerId = :partnerId))" +
            " AND ((:userType IS NULL) OR (:userType IS NOT NULL AND p.userType like %:userType%))"+
            " AND ((:isActive IS NULL) OR (:isActive IS NOT NULL AND p.isActive = :isActive))")
    Page<PartnerUser> findPartnerUsers(Long partnerId,String userType,Boolean isActive, Pageable pageable);



    @Query("SELECT p FROM PartnerUser p WHERE ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND p.partnerId = :partnerId))" +
            " AND ((:userType IS NULL) OR (:userType IS NOT NULL AND p.userType like %:userType%))"+
            " AND ((:isActive IS NULL) OR (:isActive IS NOT NULL AND p.isActive = :isActive))")
    List<PartnerUser> findPartnerUsersList(Long partnerId, String userType,Boolean isActive);
}
