package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.PartnerUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerUserRepository extends JpaRepository<PartnerUser, Long> {


//    @Query("SELECT s FROM PartnerUser s WHERE ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND s.partnerId = :partnerId))")
//    Page<PartnerUser> findPartnerUser(Long partnerId, Pageable pageable);
}
