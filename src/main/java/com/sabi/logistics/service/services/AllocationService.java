package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.AuditTrailService;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.AuditTrailFlag;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.framework.utils.Utility;
import com.sabi.logistics.core.dto.request.AllocationsDto;
import com.sabi.logistics.core.dto.response.AllocationResponseDto;
import com.sabi.logistics.core.models.AllocationHistory;
import com.sabi.logistics.core.models.Allocations;
import com.sabi.logistics.core.models.BlockType;
import com.sabi.logistics.core.models.Warehouse;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.AllocationHistoryRepository;
import com.sabi.logistics.service.repositories.AllocationsRepository;
import com.sabi.logistics.service.repositories.BlockTypeRepository;
import com.sabi.logistics.service.repositories.WarehouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
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

    @Autowired
    private AllocationHistoryRepository allocationHistoryRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;
    private final AuditTrailService auditTrailService;

    public AllocationService(ModelMapper mapper, ObjectMapper objectMapper, Validations validations,
                             AuditTrailService auditTrailService) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
        this.auditTrailService = auditTrailService;
    }

    public AllocationResponseDto createAllocation(AllocationsDto request,HttpServletRequest request1) {
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

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Create new allocation  by :" + userCurrent.getUsername(),
                        AuditTrailFlag.CREATE,
                        " Create new allocation for:" + allocationHistory.getName() ,1, Utility.getClientIp(request1));
        return mapper.map(allocationHistory, AllocationResponseDto.class);
    }

    public AllocationResponseDto updateAllocations(AllocationsDto request,HttpServletRequest request1) {
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

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Update allocation by username:" + userCurrent.getUsername(),
                        AuditTrailFlag.UPDATE,
                        " Update allocation Request for:" + allocations.getId(),1, Utility.getClientIp(request1));
        return mapper.map(allocations, AllocationResponseDto.class);
    }


    public AllocationResponseDto findAllocations(Long id){
        Allocations allocations  = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested allocations Id does not exist!"));
       List <AllocationHistory> allocationHistory = allocationHistoryRepository.findByAllocationId(id);
       AllocationResponseDto allocationResponseDto = mapper.map(allocations,AllocationResponseDto.class);
//        mapper.map(allocations,AllocationResponseDto.class);
        allocationResponseDto.setHistorys(allocationHistory);
        return allocationResponseDto;
    }



    public Page<Allocations> findAll(String name, Long wareHouseId, Long blockTypeId, String status, Long clientId, PageRequest pageRequest ){
        Page<Allocations> assetTypeProperties = repository.findAllocations(name,wareHouseId,blockTypeId,status,clientId,pageRequest);
        if(assetTypeProperties == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return assetTypeProperties;
    }



    public void enableDisEnable (EnableDisEnableDto request,HttpServletRequest request1){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Allocations allocations  = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested asset type Id does not exist!"));
        allocations.setIsActive(request.isActive());
        allocations.setUpdatedBy(userCurrent.getId());


        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Disable/Enable allocation by :" + userCurrent.getUsername() ,
                        AuditTrailFlag.UPDATE,
                        " Disable/Enable allocation Request for:" +  allocations.getId()
                                + " " +  allocations.getName(),1, Utility.getClientIp(request1));
        repository.save(allocations);

    }


    public List<Allocations> getAll(Boolean isActive){
        List<Allocations> allocations = repository.findByIsActive(isActive);
        return allocations;

    }
}
