package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.WarehouseUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseUserRepository extends JpaRepository<WarehouseUser, Long> {

    WarehouseUser findByUserId(Long userId);

    @Query("SELECT w FROM WarehouseUser w WHERE ((:wareHouseId IS NULL) OR (:wareHouseId IS NOT NULL AND w.wareHouseId = :wareHouseId))" +
            " AND ((:isActive IS NULL) OR (:isActive IS NOT NULL AND w.isActive = :isActive))")
    List<WarehouseUser> findByWareHouseIdAndIsActive(Long wareHouseId, Boolean isActive);

}
