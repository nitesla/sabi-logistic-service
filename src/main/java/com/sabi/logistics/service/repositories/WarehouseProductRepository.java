package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.WarehouseProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseProductRepository extends JpaRepository<WarehouseProduct, Long> {

    WarehouseProduct findByThirdPartyProductID(String thirdPartyProductId);

    List<WarehouseProduct> findByIsActive(Boolean isActive);

    @Query("SELECT s FROM WarehouseProduct s WHERE ((:warehouseId IS NULL) OR (:warehouseId IS NOT NULL AND s.warehouseId = :warehouseId))" +
            " AND ((:thirdPartyProductID IS NULL) OR (:thirdPartyProductID IS NOT NULL AND s.thirdPartyProductID like %:thirdPartyProductID%))" +
            " AND ((:productName IS NULL) OR (:productName IS NOT NULL AND s.productName like %:productName%)) order by s.id desc"
    )
    Page<WarehouseProduct> findAllWarehouseProducts(@Param("warehouseId") Long warehouseId,
                                      @Param("thirdPartyProductID") String thirdPartyProductID,
                                      @Param("productName") String productName,
                                      Pageable pageable);
}
