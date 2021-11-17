package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.InventoryDto;
import com.sabi.logistics.core.dto.response.InventoryResponseDto;
import com.sabi.logistics.core.models.Inventory;
import com.sabi.logistics.core.models.Partner;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.InventoryRepository;
import com.sabi.logistics.service.repositories.PartnerRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class InventoryService {
    @Autowired
    private InventoryRepository repository;
    @Autowired
    private PartnerRepository partnerRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;

//    @Autowired
//    private PartnerAssetRepository partnerAssetRepository;
//
//    @Autowired
//    private DriverRepository driverRepository;


    public InventoryService(ModelMapper mapper, ObjectMapper objectMapper, Validations validations) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
    }

    public InventoryResponseDto createInventory(InventoryDto request) {
        validations.validateInventory(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Inventory inventory = mapper.map(request,Inventory.class);
        Inventory countryExist = repository.findByShippingId(request.getShippingId());
        if(countryExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " inventory already exist");
        }
        inventory.setCreatedBy(userCurrent.getId());
        inventory.setIsActive(true);
        inventory = repository.save(inventory);
        log.debug("Create new inventory - {}"+ new Gson().toJson(inventory));
        return mapper.map(inventory, InventoryResponseDto.class);
    }


    public InventoryResponseDto updateInventory(InventoryDto request) {
        validations.validateInventory(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Inventory inventory = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested inventory Id does not exist!"));
        mapper.map(request, inventory);
        inventory.setUpdatedBy(userCurrent.getId());
        repository.save(inventory);
        log.debug("Country record updated - {}"+ new Gson().toJson(inventory));
        return mapper.map(inventory, InventoryResponseDto.class);
    }




    public InventoryResponseDto findInventoryById(Long id){
        Inventory country  = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested inventory Id does not exist!"));
        return mapper.map(country,InventoryResponseDto.class);
    }



    public Page<Inventory> findAll(Long thirdPartyId, String productName,  BigDecimal totalAmount, String status, String deliveryPartnerName,String deliveryPartnerEmail, String deliveryPartnerPhone, Long partnerId, Long shippingId, PageRequest pageRequest ){
        Page<Inventory> country = repository.findInventory(thirdPartyId,productName,totalAmount,status,deliveryPartnerName,deliveryPartnerEmail,deliveryPartnerPhone,partnerId,shippingId,pageRequest);
        if(country == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return country;

    }

    public void enableDisEnableState (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Inventory country = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Country Id does not exist!"));
        country.setIsActive(request.isActive());
        country.setUpdatedBy(userCurrent.getId());
        repository.save(country);

    }


    public List<Inventory> getAll(Boolean isActive){
        List<Inventory> countries = repository.findByIsActive(isActive);
        return countries;

    }
}
