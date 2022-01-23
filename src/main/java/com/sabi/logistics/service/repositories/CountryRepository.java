package com.sabi.logistics.service.repositories;



import com.sabi.logistics.core.models.Country;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {

    Country findByName(String name);

    @Query("SELECT c FROM Country c WHERE ((:name IS NULL) OR (:name IS NOT NULL AND c.name like %:name%))" +
            " AND ((:code IS NULL) OR (:code IS NOT NULL AND c.code like %:code%)) order by c.id desc")
    List<Country> findAllByNameAndCode(@Param("name") String name,
                                       @Param("code") String code);

    @Query("SELECT c FROM Country c WHERE ((:name IS NULL) OR (:name IS NOT NULL AND c.name like %:name%))" +
            " AND ((:code IS NULL) OR (:code IS NOT NULL AND c.code like %:code%)) order by c.id desc")
    Page<Country> findCountries(@Param("name") String name,
                                @Param("code") String code,
                                Pageable pageable);

}
