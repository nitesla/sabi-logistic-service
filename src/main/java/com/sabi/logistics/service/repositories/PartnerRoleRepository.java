package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.PartnerRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartnerRoleRepository extends JpaRepository<PartnerRole, Long> {

    PartnerRole findByRoleId (Long roleId);
    PartnerRole findByPartnerId(Long partnerId);


}
