package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.State;
import com.sabi.logistics.core.models.TollPrices;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TollPricesRepository extends JpaRepository<TollPrices, Long> {

   TollPrices findByAssestTypeId(Long id);

    @Query("SELECT s FROM TollPrices s WHERE ((:routeLocationId IS NULL) OR (:routeLocationId IS NOT NULL AND s.routeLocationId =:routeLocationId))" +
            " AND ((:assestTypeId IS NULL) OR (:assestTypeId IS NOT NULL AND s.assestTypeId = :assestTypeId)) order by s.id desc")
    Page<TollPrices> findTollPrices(@Param("routeLocationId") Long routeLocationId,
                           @Param("assestTypeId") Long assestTypeId,
                           Pageable pageable);
    List<State> findByIsActive(Boolean isActive);
}
