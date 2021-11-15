package com.sabi.logistics.service.repositories;




import com.sabi.logistics.core.models.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long>, JpaSpecificationExecutor<OrderItem> {


    OrderItem findByName(String name);

    List<OrderItem> findByIsActive(Boolean isActive);

    List<OrderItem> findByOrderID(Long orderID);


}
