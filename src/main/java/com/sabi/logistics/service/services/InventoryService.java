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
import com.sabi.logistics.core.models.OrderItem;
import com.sabi.logistics.core.models.Warehouse;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.InventoryRepository;
import com.sabi.logistics.service.repositories.OrderItemRepository;
import com.sabi.logistics.service.repositories.WarehouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@SuppressWarnings("All")
@Slf4j
@Service
public class InventoryService {
    @Autowired
    private InventoryRepository repository;
    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;




    public InventoryService(ModelMapper mapper, ObjectMapper objectMapper, Validations validations) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
    }

    public InventoryResponseDto createInventory(InventoryDto request) {
        validations.validateInventory(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Inventory inventory = mapper.map(request,Inventory.class);
        Inventory inventoryExist = repository.findByShippingId(request.getShippingId());
        if(inventoryExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " inventory already exist");
        }
        Warehouse warehouse = warehouseRepository.findWarehouseById(request.getWareHouseId());
        log.info("Checking ware house ::::::::::::::::::::::::: " + warehouse);
        if(warehouse ==null){
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " warehouse not found");
        }

        if (request.getOrderItemId() != null) {
            request.getOrderItemId().forEach(id -> {
                OrderItem exist = orderItemRepository.findOrderItemById(id);
                if (exist == null) {
                    throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Order Item Id does not exist");
                }
            });
        }
        inventory.setCreatedBy(userCurrent.getId());
        inventory.setIsActive(true);
        inventory = repository.save(inventory);
        log.debug("Create new inventory - {}"+ new Gson().toJson(inventory));
        InventoryResponseDto inventoryResponseDto = mapper.map(inventory, InventoryResponseDto.class);

        if (request.getOrderItemId() != null) {
            request.getOrderItemId().forEach(id -> {
                OrderItem orderItem = orderItemRepository.findOrderItemById(id);
                orderItem.setInventoryId(inventoryResponseDto.getId());
                orderItemRepository.save(orderItem);

            });
        }

        return inventoryResponseDto;
    }


    public InventoryResponseDto updateInventory(InventoryDto request) {
        validations.validateInventory(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Inventory inventory = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested inventory Id does not exist!"));
        Warehouse warehouse = warehouseRepository.findWarehouseById(request.getWareHouseId());
        if(warehouse ==null){
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " warehouse not found");
        }
        mapper.map(request, inventory);
        inventory.setUpdatedBy(userCurrent.getId());
//        inventory.setWareHouseName(warehouse.getName());
        repository.save(inventory);
        log.debug("Country record updated - {}"+ new Gson().toJson(inventory));
        return mapper.map(inventory, InventoryResponseDto.class);
    }




    public InventoryResponseDto findInventoryById(Long id){
        Inventory inventory  = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested inventory Id does not exist!"));
        Warehouse savedWarehouse = warehouseRepository.findWarehouseById(inventory.getWareHouseId());
        inventory.setWarehouseName(savedWarehouse.getName());
        log.info("fetched inventory ::::::::::::::::::::::::::::: " + inventory);
        return mapper.map(inventory,InventoryResponseDto.class);
    }



    public Page<Inventory> findAll(Long thirdPartyId, String productName,  BigDecimal totalAmount, String status, String deliveryPartnerName,String deliveryPartnerEmail, String deliveryPartnerPhone, Long partnerId, Long shippingId,Long wareHouseId, PageRequest pageRequest ){
        Page<Inventory> inventories = repository.findInventory(thirdPartyId,productName,totalAmount,status,deliveryPartnerName,deliveryPartnerEmail,deliveryPartnerPhone,partnerId,shippingId,wareHouseId,pageRequest);
        if(inventories == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        inventories.forEach(inventory -> {
            Warehouse savedWareHouse = warehouseRepository.findWarehouseById(inventory.getWareHouseId());
            inventory.setWarehouseName(savedWareHouse.getName());
        });
        return inventories;

    }

    public void enableDisEnableState (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Inventory inventory = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Country Id does not exist!"));
        inventory.setIsActive(request.isActive());
        inventory.setUpdatedBy(userCurrent.getId());
        repository.save(inventory);

    }


    public List<Inventory> getAll(Boolean isActive){
        List<Inventory> inventories = repository.findByIsActive(isActive);

        return inventories;

    }
}
