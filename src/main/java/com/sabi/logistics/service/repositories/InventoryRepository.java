//package com.sabi.logistics.service.repositories;
//
//import com.sabi.logistics.core.models.LGA;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//@Repository
//public interface InventoryRepository extends JpaRepository<Inventory, Long> {
//
//    Invetory findByName(String name);
//    List<Country> findByIsActive(Boolean isActive);
//
//    @Query("SELECT c FROM Country c WHERE ((:name IS NULL) OR (:name IS NOT NULL AND c.name = :name))" +
//            " AND ((:code IS NULL) OR (:code IS NOT NULL AND c.code = :code))")
//    Page<Country> findCountries(@Param("name") String name,
//                                @Param("code") String code,
//                                Pageable pageable);
//}
