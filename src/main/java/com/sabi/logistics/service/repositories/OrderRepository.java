package com.sabi.logistics.service.repositories;



import com.sabi.logistics.core.models.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {


    Order findByReferenceNo(String referenceNo);

    List<Order> findByIsActive(Boolean isActive);


}
