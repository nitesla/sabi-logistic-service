package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.WarehouseRequestDto;
import com.sabi.logistics.core.dto.response.WarehouseResponseDto;
import com.sabi.logistics.core.models.Warehouse;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.WarehouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
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

    public WarehouseService(WarehouseRepository WarehouseRepository, ModelMapper mapper, ObjectMapper objectMapper, Validations validations) {
        this.warehouseRepository = WarehouseRepository;
        this.mapper = mapper;
        this.validations = validations;
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
        return mapper.map(warehouse, WarehouseResponseDto.class);
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
        return mapper.map(warehouse, WarehouseResponseDto.class);
    }

    public WarehouseResponseDto findWarehouse(Long id) {
        Warehouse Warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Warehouse Id does not exist!"));
        return mapper.map(Warehouse, WarehouseResponseDto.class);
    }


    public Page<Warehouse> findAll(String owner, String name, long partnerId, long lgaId, PageRequest pageRequest) {
        Page<Warehouse> warehouse = warehouseRepository.findWarehouse(owner, name, partnerId, lgaId, pageRequest);
        if (warehouse == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
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


    public List<Warehouse> getAll(Boolean isActive) {
        List<Warehouse> warehouses = warehouseRepository.findByIsActive(isActive);
        return warehouses;

    }
}
