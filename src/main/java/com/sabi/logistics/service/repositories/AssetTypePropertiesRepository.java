package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.AssetTypeProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetTypePropertiesRepository extends JpaRepository<AssetTypeProperties, Long> {

    AssetTypeProperties findByName(String name);

    AssetTypeProperties findAssetTypePropertiesById(Long Id);

    List<AssetTypeProperties> findByIsActive(Boolean isActive);

    @Query("SELECT a FROM AssetTypeProperties a WHERE ((:name IS NULL) OR (:name IS NOT NULL AND a.name like %:name%)) order by a.id desc")
    Page<AssetTypeProperties> findAssets(@Param("name") String name, Pageable pageable);
}
