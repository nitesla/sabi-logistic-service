package com.sabi.logistics.service.repositories;


import com.sabi.logistics.core.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 *
 * This interface is responsible for Product crud operations
 */

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

       Product findByName(String name);

       Product findByNameAndThirdPartyId(String name, Long thirdPartyId);

       List<Product> findByThirdPartyIdAndIsActive(Long thirdPartyId, Boolean isActive);


}
