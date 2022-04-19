package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.WarehouseRequestDto;
import com.sabi.logistics.core.dto.response.WarehouseResponseDto;
import com.sabi.logistics.core.models.Partner;
import com.sabi.logistics.core.models.Warehouse;
import com.sabi.logistics.core.models.WarehouseProduct;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class WarehouseService {
    private final WarehouseRepository warehouseRepository;
    private final ModelMapper mapper;
    private final Validations validations;

    @Autowired
    private  LGARepository lgaRepository;
    @Autowired
    private StateRepository stateRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PartnerRepository partnerRepository;

    private final WarehouseProductRepository warehouseProductRepository;

    public WarehouseService(WarehouseRepository WarehouseRepository, ModelMapper mapper, ObjectMapper objectMapper, Validations validations, WarehouseProductRepository warehouseProductRepository) {
        this.warehouseRepository = WarehouseRepository;
        this.mapper = mapper;
        this.validations = validations;
        this.warehouseProductRepository = warehouseProductRepository;
    }

    public WarehouseResponseDto createWarehouse(WarehouseRequestDto request) {
        validations.validateWarehouse(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Warehouse warehouse = mapper.map(request, Warehouse.class);
        boolean warehouseExists = warehouseRepository.exists(Example.of(Warehouse.builder().contactPhone(request.getContactPhone())
                .address(request.getAddress())
                .build()));
        if (warehouseExists) {
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Warehouse already exist");
        }
        warehouse.setCreatedBy(userCurrent.getId());
        warehouse.setIsActive(true);
        warehouse = warehouseRepository.save(warehouse);
        log.debug("Create new warehouse - {}" + new Gson().toJson(warehouse));
        WarehouseResponseDto warehouseResponseDto = mapper.map(warehouse, WarehouseResponseDto.class);
        warehouseResponseDto.setStockLeft(warehouseResponseDto.getTotalStock() - warehouseResponseDto.getStockSold());
        return warehouseResponseDto;
    }

    public WarehouseResponseDto updateWarehouse(WarehouseRequestDto request) {
        validations.validateWarehouse(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Warehouse warehouse = warehouseRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested warehouse Id does not exist!"));
        mapper.map(request, warehouse);
        warehouse.setUpdatedBy(userCurrent.getId());
        warehouseRepository.save(warehouse);
        log.debug("warehouse record updated - {}" + new Gson().toJson(warehouse));
        WarehouseResponseDto warehouseResponseDto = mapper.map(warehouse, WarehouseResponseDto.class);
        warehouseResponseDto.setStockLeft(warehouseResponseDto.getTotalStock() - warehouseResponseDto.getStockSold());
        return warehouseResponseDto;
    }

    public WarehouseResponseDto findWarehouse(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Warehouse Id does not exist!"));
//        LGA lga  =  lgaRepository.findLGAById (warehouse.getLgaId());
//        warehouse.setLgaName(lga.getName());
//        State state = stateRepository.findStateById(lga.getStateId());
//        warehouse.setStateName(state.getName());
        List<WarehouseProduct> warehouseProductsList = warehouseProductRepository.findByWarehouseId(warehouse.getId());
        if (warehouseProductsList != null) {
            warehouse.setProductInfo(warehouseProductsList);
        }
        WarehouseResponseDto warehouseResponseDto = mapper.map(warehouse, WarehouseResponseDto.class);
        warehouseResponseDto.setStockLeft(warehouseResponseDto.getTotalStock() - warehouseResponseDto.getStockSold());
//        warehouseResponseDto.setLgaId(lga.getId());
//        warehouseResponseDto.setStateId(state.getId());
        Partner partner = partnerRepository.findPartnerById(warehouse.getPartnerId());
        User user = userRepository.getOne(partner.getUserId());
        warehouseResponseDto.setWareHouseManager(user.getLastName() + " " + user.getFirstName());
        return warehouseResponseDto;
    }

    /**
     * Adds 'state' parameter to query by state in WareHouseRepository
     * @Author: Afam Okonkwo
     * @Date: 19/04/2022
     * @param owner
     * @param name
     * @param partnerId
     * @param state
     * @param isActive
     * @param lgaId
     * @param pageRequest
     * @return
     */
    public Page<Warehouse> findAll(String owner, String name, Long partnerId, String state, Boolean isActive,Long lgaId, PageRequest pageRequest) {
    //public Page<Warehouse> findAll(String owner, String name, Long partnerId, Boolean isActive,Long lgaId, PageRequest pageRequest)
        Page<Warehouse> warehouse = warehouseRepository.findWarehouse(owner, name, partnerId, state,isActive, lgaId, pageRequest);
        if (warehouse == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        warehouse.forEach(warehouse1 ->{
//                LGA   lga  =  lgaRepository.findLGAById (warehouse1.getLgaId());
//        warehouse1.setLgaName(lga.getName());
//        State state = stateRepository.getOne(lga.getStateId());
//            warehouse1.setStateName(state.getName());
            List<WarehouseProduct> warehouseProductsList = warehouseProductRepository.findByWarehouseId(warehouse1.getId());
            if (warehouseProductsList != null) {
                warehouse1.setProductInfo(warehouseProductsList);
            }
        });
        return warehouse;

    }


    public void enableDisEnableState(EnableDisEnableDto request) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Warehouse Warehouse = warehouseRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Warehouse Id does not exist!"));
        Warehouse.setIsActive(request.isActive());
        Warehouse.setUpdatedBy(userCurrent.getId());
        warehouseRepository.save(Warehouse);
    }


    public List<Warehouse> getAll(Boolean isActive,Long partnerId) {
        List<Warehouse> warehouses = warehouseRepository.findWarehouses(isActive,partnerId);
        warehouses.stream().forEach(warehouse -> {
            List<WarehouseProduct> warehouseProductsList = warehouseProductRepository.findByWarehouseId(warehouse.getId());
            if (warehouseProductsList != null)
                warehouse.setProductInfo(warehouseProductsList);
        });
        return warehouses;
    }
}
