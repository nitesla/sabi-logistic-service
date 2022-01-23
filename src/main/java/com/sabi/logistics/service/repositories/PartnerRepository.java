package com.sabi.logistics.service.repositories;



import com.sabi.logistics.core.models.Partner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface PartnerRepository extends JpaRepository<Partner, Long> {


    Partner findByName(String name);

    Partner findByRegistrationToken (String registrationToken);

    Partner findByUserId(Long userId);

    Partner findPartnerById(Long id);
    Partner findPartnerPropertiesById(Long id);

    Partner findByPhone(String phone);

    List<Partner> findByIsActive(Boolean isActive);

    @Query("SELECT p FROM Partner p WHERE ((:name IS NULL) OR (:name IS NOT NULL AND p.name like %:name%)) order by p.id desc")
    Page<Partner> findPartnersProperties(@Param("name") String name, Pageable pageable);


    Partner findBySupplierId (Long supplierId);
}
