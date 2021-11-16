package com.sabi.logistics.service.repositories;




import com.sabi.logistics.core.models.DeliveryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface DeliveryItemRepository extends JpaRepository<DeliveryItem, Long>, JpaSpecificationExecutor<DeliveryItem> {


    DeliveryItem findByDeliveryIDAndTripRequestID(Long deliveryId, Long tripRequestId);


    List<DeliveryItem> findByIsActive(Boolean isActive);


}
