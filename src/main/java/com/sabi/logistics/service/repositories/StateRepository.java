package com.sabi.logistics.service.repositories;




import com.sabi.logistics.core.models.State;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 *
 * This interface is responsible for State crud operations
 */

@Repository
public interface StateRepository extends JpaRepository<State, Long> {

    State findByName(String name);

    State findStateById(Long Id);

    @Query("SELECT s FROM State s WHERE ((:countryId IS NULL) OR (:countryId IS NOT NULL AND s.countryId = :countryId))")
    List<State> findByCountryId(Long countryId);
    List<State> findByIsActive(Boolean isActive);


    @Query("SELECT s FROM State s WHERE ((:name IS NULL) OR (:name IS NOT NULL AND s.name like %:name%))" +
            " AND ((:countryId IS NULL) OR (:countryId IS NOT NULL AND s.countryId = :countryId)) order by s.id desc")
    Page<State> findStates(@Param("name") String name,
                           @Param("countryId") Long countryId,
                           Pageable pageable);

}
