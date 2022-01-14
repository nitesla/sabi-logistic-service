package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {


    List<Client> findByIsActive(Boolean isActive);
    Client findClientById(Long id);

    @Query("SELECT s FROM Client s WHERE ((:userId IS NULL) OR (:userId IS NOT NULL AND s.userId = :userId)) order by s.id desc " )
//            " AND ((:userId IS NULL) OR (:userId IS NOT NULL AND s.userId = :userId))")
    Page<Client> findAllClients(
                                @Param("userId")Long userId, Pageable pageable);
}


