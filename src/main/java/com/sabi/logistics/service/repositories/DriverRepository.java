package com.sabi.logistics.service.repositories;

import com.sabi.logistics.core.models.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {


    Driver findByUserId(Long userId);

    Driver findDriverById(Long Id);

}
