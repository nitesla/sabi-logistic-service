package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.TripItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TripItemRepository extends JpaRepository<TripItem, Long>, JpaSpecificationExecutor<TripItem> {


    List<TripItem> findByIsActive(Boolean isActive);

    TripItem findByTripRequestIdAndThirdPartyProductId(Long tripRequestId, Long thirdPartyProductId);

    List<TripItem> findByTripRequestId(Long Id);

    @Query("SELECT t FROM TripItem t WHERE ((:thirdPartyProductId IS NULL) OR (:thirdPartyProductId IS NOT NULL AND t.thirdPartyProductId = :thirdPartyProductId))" +
            " AND ((:tripRequestId IS NULL) OR (:tripRequestId IS NOT NULL AND t.tripRequestId = :tripRequestId))" +
            " AND ((:productName IS NULL) OR (:productName IS NOT NULL AND t.productName like %:productName%)) order by t.id desc")
    Page<TripItem> findByTripItem(@Param("thirdPartyProductId") Long thirdPartyProductId,
                                                      @Param("tripRequestId") Long tripRequestId, @Param("productName") String productName,
                                                      Pageable pageable);


}
