package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.RequestResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface RequestResponseRepository extends JpaRepository<RequestResponse, Long>, JpaSpecificationExecutor<RequestResponse> {


    List<RequestResponse> findByIsActive(Boolean isActive);

    RequestResponse findByTripRequestIDAndPartnerID(Long tripRequestID, Long partnerID);

    List<RequestResponse> findByTripRequestID(Long id);

}
