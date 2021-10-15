package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {


    List<Client> findByIsActive(Boolean isActive);
    Client findClientById(Long id);

    @Query("SELECT s FROM Client s WHERE ((:id IS NULL) OR (:id IS NOT NULL AND s.id = :id))")
    Page<Client> findAllClients(@Param("id") Long id, Pageable pageable);
}


