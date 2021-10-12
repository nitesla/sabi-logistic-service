package com.sabilogistics.service.repositories;

import com.sabilogisticscore.models.PartnerCategories;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerCategoriesRepository extends JpaRepository<PartnerCategories, Long> {

    PartnerCategories findByPartnerId(Long partnerId);
    PartnerCategories findByCategoryIdId(Long categoryId);
    List<PartnerCategories> findByIsActive(Boolean isActive);

    @Query("SELECT c FROM PartnerCategories c WHERE ((:partnerId IS NULL) OR (:partnerId IS NOT NULL AND c.partnerId = :partnerId))" +
            " AND ((:categoryId IS NULL) OR (:categoryId IS NOT NULL AND c.categoryId = :categoryId))")
    Page<PartnerCategories> findPartnerCategories(@Param("partnerId") Long partnerId,
                                          @Param("categoryId") Long categoryId,
                                          Pageable pageable);
}
