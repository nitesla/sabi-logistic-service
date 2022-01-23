package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.PartnerAssetPicture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface PartnerAssetPictureRepository extends JpaRepository<PartnerAssetPicture, Long> {

    PartnerAssetPicture findByPartnerAssetIdAndPictureType(Long partnerAssetId,String pictureType);

    List<PartnerAssetPicture> findByIsActive(Boolean isActive);

    List<PartnerAssetPicture> findByPartnerAssetId(Long partnerAssetId);


    @Query("SELECT c FROM PartnerAssetPicture c WHERE ((:partnerAssetId IS NULL) OR (:partnerAssetId IS NOT NULL AND c.partnerAssetId = :partnerAssetId))" +
            " AND ((:pictureType IS NULL) OR (:pictureType IS NOT NULL AND c.pictureType like %:pictureType%))")
    Page<PartnerAssetPicture> findAssetPicture(@Param("partnerAssetId") Long partnerAssetId,
                                @Param("pictureType") String pictureType,
                                Pageable pageable);

}
