package com.sabi.logistics.service.repositories;



import com.sabi.logistics.core.models.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long>, JpaSpecificationExecutor<Delivery> {


    List<Delivery> findByIsActive(Boolean isActive);

    Delivery findByOrderItemIDAndPartnerAssetID(Long orderItemId, Long partnerAssetId );


}
