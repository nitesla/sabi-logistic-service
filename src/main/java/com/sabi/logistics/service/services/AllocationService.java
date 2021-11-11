package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.AllocationHistoryDto;
import com.sabi.logistics.core.dto.request.AllocationsDto;
import com.sabi.logistics.core.dto.response.AllocationHistoryResponseDto;
import com.sabi.logistics.core.dto.response.AllocationResponseDto;
import com.sabi.logistics.core.models.Allocations;
import com.sabi.logistics.core.models.BlockType;
import com.sabi.logistics.core.models.Warehouse;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.AllocationsRepository;
import com.sabi.logistics.service.repositories.BlockTypeRepository;
import com.sabi.logistics.service.repositories.WarehouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class AllocationService {

    @Autowired
    private AllocationsRepository repository;
//    private ClientRepository clientRepository;
    @Autowired
    private BlockTypeRepository blockTypeRepository;
    @Autowired
    private WarehouseRepository warehouseRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;

    public AllocationService(ModelMapper mapper, ObjectMapper objectMapper, Validations validations) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
    }

    public AllocationResponseDto createAllocation(AllocationsDto request) {
//        validations.validateAssetTypeProperties(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Allocations allocationHistory = mapper.map(request,Allocations.class);
        Allocations exist = repository.findByName(request.getName());
        if(exist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Allocation already exist");
        }
        Warehouse savedWareHouse = warehouseRepository.findById(request.getBlockTypeId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested warehouse Id does not exist!"));
        BlockType savedBlockType = blockTypeRepository.findById(request.getBlockTypeId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested block type Id does not exist!"));
        allocationHistory.setCreatedBy(userCurrent.getId());
        allocationHistory.setIsActive(true);
        allocationHistory = repository.save(allocationHistory);
        log.debug("Create new asset type - {}"+ new Gson().toJson(allocationHistory));
        return mapper.map(allocationHistory, AllocationResponseDto.class);
    }

    public AllocationResponseDto updateAllocations(AllocationsDto request) {
//        validations.validateAssetTypeProperties(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Allocations allocations = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested allocations Id does not exist!"));
        Warehouse savedWareHouse = warehouseRepository.findById(request.getBlockTypeId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested warehouse Id does not exist!"));
        BlockType savedBlockType = blockTypeRepository.findById(request.getBlockTypeId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested block type Id does not exist!"));
        mapper.map(request, allocations);
        allocations.setUpdatedBy(userCurrent.getId());
        repository.save(allocations);
        log.debug("Allocations record updated - {}"+ new Gson().toJson(allocations));
        return mapper.map(allocations, AllocationResponseDto.class);
    }


    public AllocationResponseDto findAllocations(Long id){
        Allocations allocations  = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested allocations Id does not exist!"));
        return mapper.map(allocations,AllocationResponseDto.class);
    }



    public Page<Allocations> findAll(String name, Long wareHouseId, Long blockTypeId, String status, Long clientId, PageRequest pageRequest ){
        Page<Allocations> assetTypeProperties = repository.findAllocations(name,wareHouseId,blockTypeId,status,clientId,pageRequest);
        if(assetTypeProperties == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return assetTypeProperties;
    }



    public void enableDisEnable (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Allocations allocations  = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested asset type Id does not exist!"));
        allocations.setIsActive(request.isActive());
        allocations.setUpdatedBy(userCurrent.getId());
        repository.save(allocations);

    }


    public List<Allocations> getAll(Boolean isActive){
        List<Allocations> allocations = repository.findByIsActive(isActive);
        return allocations;

    }
}
