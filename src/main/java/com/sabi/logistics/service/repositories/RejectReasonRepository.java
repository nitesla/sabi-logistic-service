package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.RejectReason;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RejectReasonRepository extends JpaRepository<RejectReason, Long> {

    RejectReason findByName(String name);

    @Query("SELECT d FROM RejectReason d WHERE ((:name IS NULL) OR (:name IS NOT NULL AND d.name = :name)) order by d.id desc "
            )
    Page<RejectReason> findRejectReason(@Param("name") String name, Pageable pageable);

}
