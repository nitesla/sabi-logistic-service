package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.WarehouseProductDto;
import com.sabi.logistics.core.dto.response.WarehouseProductResponseDto;
import com.sabi.logistics.core.models.WarehouseProduct;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.WarehouseProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@SuppressWarnings("All")
@Service
@Slf4j
public class WarehouseProductService {
    @Autowired
    private WarehouseProductRepository repository;
    @Autowired
    private ModelMapper mapper;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private Validations validations;

    /** <summary>
     * warehouse product creation
     * </summary>
     * <remarks>this method is responsible for creation of new warehouse product</remarks>
     */

    public WarehouseProductResponseDto createWarehouseProduct(WarehouseProductDto request) {
        validations.validateWarehouseProduct(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        WarehouseProduct warehouseProduct = mapper.map(request,WarehouseProduct.class);
        WarehouseProduct warehouseProductExist = repository.findByThirdPartyProductID(request.getThirdPartyProductID());
        if(warehouseProductExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " warehouse product already exist");
        }
        warehouseProduct.setCreatedBy(userCurrent.getId());
        warehouseProduct.setIsActive(true);
        warehouseProduct = repository.save(warehouseProduct);
        log.debug("Create new State - {}"+ new Gson().toJson(warehouseProduct));
        return mapper.map(warehouseProduct, WarehouseProductResponseDto.class);
    }


    /** <summary>
     * warehouse product update
     * </summary>
     * <remarks>this method is responsible for updating already existing warehouse product</remarks>
     */

    public WarehouseProductResponseDto updateWarehouseProduct(WarehouseProductDto request) {
        validations.validateWarehouseProduct(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        WarehouseProduct warehouseProduct = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested warehouse product Id does not exist!"));
        mapper.map(request, warehouseProduct);
        warehouseProduct.setUpdatedBy(userCurrent.getId());
        repository.save(warehouseProduct);
        log.debug("State record warehouse product - {}"+ new Gson().toJson(warehouseProduct));
        return mapper.map(warehouseProduct, WarehouseProductResponseDto.class);
    }


    /** <summary>
     * Find warehouse product
     * </summary>
     * <remarks>this method is responsible for getting a single record</remarks>
     */
    public WarehouseProductResponseDto findWarehouseProduct(Long id){
        WarehouseProduct warehouseProduct = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested warehouse product Id does not exist!"));
//        Country country = countryRepository.getOne(state.getCountryId());
//        state.setCountryName(country.getName());
        return mapper.map(warehouseProduct,WarehouseProductResponseDto.class);
    }

    public WarehouseProductResponseDto findWarehouseProductByThirdPartyProductId(String thirdpartyProductId){
        WarehouseProduct warehouseProduct = repository.findByThirdPartyProductID(thirdpartyProductId);
        if(warehouseProduct == null){
            throw  new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested warehouse product Id does not exist!");
        }
//        Country country = countryRepository.getOne(state.getCountryId());
//        state.setCountryName(country.getName());
        return mapper.map(warehouseProduct,WarehouseProductResponseDto.class);
    }


    /** <summary>
     * Find all warehouse product
     * </summary>
     * <remarks>this method is responsible for getting all records in pagination</remarks>
     */
    public Page<WarehouseProduct> findAll(Long warehouseId,String thirdPartyProductID,String productName,PageRequest pageRequest ){
        Page<WarehouseProduct> warehouseProducts = repository.findAllWarehouseProducts(warehouseId,thirdPartyProductID,productName,pageRequest);
        return warehouseProducts;
    }


    /** <summary>
     * Enable disenable
     * </summary>
     * <remarks>this method is responsible for enabling and dis enabling a warehouse product</remarks>
     */
    public void enableDisEnableWarehouseProduct (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        WarehouseProduct warehouseProduct = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested warehouse product Id does not exist!"));
        warehouseProduct.setIsActive(request.isActive());
        warehouseProduct.setUpdatedBy(userCurrent.getId());
        repository.save(warehouseProduct);

    }


    public List<WarehouseProduct> getAll(Boolean isActive){
        List<WarehouseProduct> warehouseProductList = repository.findByIsActive(isActive);
        return warehouseProductList;

    }
}
