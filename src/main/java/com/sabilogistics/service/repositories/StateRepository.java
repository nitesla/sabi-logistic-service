package com.sabilogistics.service.repositories;



import com.sabilogisticscore.models.State;
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

    List<State> findByIsActive(Boolean isActive);

    @Query("SELECT s FROM State s WHERE ((:name IS NULL) OR (:name IS NOT NULL AND s.name = :name))")
    Page<State> findStates(@Param("name") String name, Pageable pageable);

}
