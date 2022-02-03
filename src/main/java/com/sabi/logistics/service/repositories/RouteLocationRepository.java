package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.RouteLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteLocationRepository extends JpaRepository<RouteLocation, Long>, JpaSpecificationExecutor<RouteLocation> {

    RouteLocation findByName(String name);

    List<RouteLocation> findByStateId(Long StateId);

    List<RouteLocation> findByIsActive(Boolean isActive);
}
