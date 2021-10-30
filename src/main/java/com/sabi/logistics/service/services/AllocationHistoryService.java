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
import com.sabi.logistics.core.dto.response.AllocationHistoryResponseDto;
import com.sabi.logistics.core.models.AllocationHistory;
import com.sabi.logistics.core.models.Allocations;
import com.sabi.logistics.core.models.Client;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.AllocationHistoryRepository;
import com.sabi.logistics.service.repositories.AllocationsRepository;
import com.sabi.logistics.service.repositories.ClientRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class AllocationHistoryService {

    @Autowired
    private AllocationHistoryRepository repository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private AllocationsRepository allocationsRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;

    public AllocationHistoryService(AllocationHistoryRepository repository, ModelMapper mapper, ObjectMapper objectMapper, Validations validations) {
        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
    }

    public AllocationHistoryResponseDto createAllocationHistory(AllocationHistoryDto request) {
//        validations.validateAssetTypeProperties(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        AllocationHistory allocationHistory = mapper.map(request,AllocationHistory.class);
        AllocationHistory exist = repository.findAllocationHistoriesById(request.getId());
        if(exist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Allocation history already exist");
        }
        Client savedClient = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested client id does not exist!"));
        Allocations savedAllocation = allocationsRepository.findById(request.getAllocationId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested allocation id does not exist!"));
        allocationHistory.setCreatedBy(userCurrent.getId());
        allocationHistory.setIsActive(true);
        allocationHistory = repository.save(allocationHistory);
        log.debug("Create new asset type - {}"+ new Gson().toJson(allocationHistory));
        return mapper.map(allocationHistory, AllocationHistoryResponseDto.class);
    }

    public AllocationHistoryResponseDto updateAllocationHistory(AllocationHistoryDto request) {
//        validations.validateAssetTypeProperties(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        AllocationHistory allocationHistory = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested allocation history Id does not exist!"));
        Client savedClient = clientRepository.findClientById(request.getClientId());
        if (savedClient == null){
            throw  new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Client Id does not exist!");
        }
        Allocations savedAllocation = allocationsRepository.findAllocationsById(request.getAllocationId());
        if (savedAllocation == null){
            throw  new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Allocation Id does not exist!");
        }
        mapper.map(request, allocationHistory);
        allocationHistory.setUpdatedBy(userCurrent.getId());
        repository.save(allocationHistory);
        log.debug("Allocation History record updated - {}"+ new Gson().toJson(allocationHistory));
        return mapper.map(allocationHistory, AllocationHistoryResponseDto.class);
    }


    public AllocationHistoryResponseDto findAllocationHistory(Long id){
        AllocationHistory allocationHistory  = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested allocation history Id does not exist!"));
        return mapper.map(allocationHistory,AllocationHistoryResponseDto.class);
    }



    public Page<AllocationHistory> findAll(Long allocationId, Long clientId, PageRequest pageRequest ){
        Page<AllocationHistory> assetTypeProperties = repository.findAllocationHistory(allocationId,clientId,pageRequest);
        if(assetTypeProperties == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return assetTypeProperties;

    }



    public void enableDisEnable (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        AllocationHistory assetTypeProperties  = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested asset type Id does not exist!"));
        assetTypeProperties.setIsActive(request.isActive());
        assetTypeProperties.setUpdatedBy(userCurrent.getId());
        repository.save(assetTypeProperties);

    }


    public List<AllocationHistory> getAll(Boolean isActive){
        List<AllocationHistory> assetTypeProperties = repository.findByIsActive(isActive);
        return assetTypeProperties;

    }
}
