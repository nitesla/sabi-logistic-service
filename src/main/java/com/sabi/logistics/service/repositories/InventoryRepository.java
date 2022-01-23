package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Inventory findByShippingId(Long shippingId);
    List<Inventory> findByIsActive(Boolean isActive);


    @Query("SELECT c FROM Inventory c WHERE ((:thirdPartyId IS NULL) OR (:thirdPartyId IS NOT NULL AND c.thirdPartyId = :thirdPartyId))" +
            " AND ((:productName IS NULL) OR (:productName IS NOT NULL AND c.productName like %:productName%))" +
//            " AND ((:qty IS NULL) OR (:qty IS NOT NULL AND c.qty = :qty))" +
            " AND ((:totalAmount IS NULL) OR (:totalAmount IS NOT NULL AND c.totalAmount = :totalAmount))" +
            " AND ((:status IS NULL) OR (:status IS NOT NULL AND c.status like %:status%))" +
            " AND ((:deliveryPartnerName IS NULL) OR (:deliveryPartnerName IS NOT NULL AND c.deliveryPartnerName like %:deliveryPartnerName%))" +
            " AND ((:deliveryPartnerEmail IS NULL) OR (:deliveryPartnerEmail IS NOT NULL AND c.deliveryPartnerEmail like %:deliveryPartnerEmail%))" +
            " AND ((:deliveryPartnerPhone IS NULL) OR (:deliveryPartnerPhone IS NOT NULL AND c.deliveryPartnerPhone like %:deliveryPartnerPhone%))" +
            " AND ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND c.partnerId = :partnerId))" +
            " AND ((:wareHouseId IS NULL) OR (:wareHouseId IS NOT NULL AND c.wareHouseId = :wareHouseId))" +
            " AND ((:shippingId IS NULL) OR (:shippingId IS NOT NULL AND c.shippingId = :shippingId)) order by c.id desc "
    )
    Page<Inventory> findInventory(@Param("thirdPartyId") Long thirdPartyId,
                                  @Param("productName") String productName,
//                                  @Param("qty") int qty,
                                  @Param("totalAmount") BigDecimal totalAmount,
                                  @Param("status") String status,
                                  @Param("deliveryPartnerName") String deliveryPartnerName,
                                  @Param("deliveryPartnerEmail") String deliveryPartnerEmail,
                                  @Param("deliveryPartnerPhone") String deliveryPartnerPhone,
                                  @Param("partnerId") Long partnerId,
                                  @Param("wareHouseId") Long wareHouseId,
                                  @Param("shippingId") Long shippingId,
                                  Pageable pageable);
}
