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
import com.sabi.logistics.core.dto.request.BlockTypeDto;
import com.sabi.logistics.core.dto.response.BlockTypeResponseDto;
import com.sabi.logistics.core.models.BlockType;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.BlockTypeRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Service
@Slf4j
public class BlockTypeService {

    private BlockTypeRepository repository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;
    private final AuditTrailService auditTrailService;

    public BlockTypeService(BlockTypeRepository repository, ModelMapper mapper, ObjectMapper objectMapper, Validations validations,
                            AuditTrailService auditTrailService) {
        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
        this.auditTrailService = auditTrailService;
    }

    public BlockTypeResponseDto createBlockType(BlockTypeDto request,HttpServletRequest request1) {
        validations.validateBlockType(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        BlockType partnerCategories = mapper.map(request,BlockType.class);
        BlockType exist = repository.findByName(request.getName());
        if(exist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " block type already exist");
        }
        partnerCategories.setCreatedBy(userCurrent.getId());
        partnerCategories.setIsActive(true);
        partnerCategories = repository.save(partnerCategories);
        log.debug("Create new block type - {}"+ new Gson().toJson(partnerCategories));

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Create new blockType  by :" + userCurrent.getUsername(),
                        AuditTrailFlag.CREATE,
                        " Create new blockType for:" + partnerCategories.getName() ,1, Utility.getClientIp(request1));
        return mapper.map(partnerCategories, BlockTypeResponseDto.class);
    }

    public BlockTypeResponseDto updateBlockType(BlockTypeDto request,HttpServletRequest request1) {
        validations.validateBlockType(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        BlockType savedBlockType = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested block type id does not exist!"));
        mapper.map(request, savedBlockType);
        savedBlockType.setUpdatedBy(userCurrent.getId());
        repository.save(savedBlockType);
        log.debug("block type record updated - {}"+ new Gson().toJson(savedBlockType));

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Update blockType by username:" + userCurrent.getUsername(),
                        AuditTrailFlag.UPDATE,
                        " Update blockType Request for:" + savedBlockType.getId(),1, Utility.getClientIp(request1));
        return mapper.map(savedBlockType, BlockTypeResponseDto.class);
    }

    public BlockTypeResponseDto findByBlockTypeId(Long id){
        BlockType partnerCategories  = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested block type id does not exist!"));
        return mapper.map(partnerCategories,BlockTypeResponseDto.class);
    }

    public BlockTypeResponseDto findBlockTypeByName(String name){
        BlockType savedBlockType  = repository.findByName(name);
        if (savedBlockType == null){
           throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested block type does not exist!");
        }
        return mapper.map(savedBlockType,BlockTypeResponseDto.class);
    }

    public Page<BlockType> findAll(String name, PageRequest pageRequest ){
        Page<BlockType> savedBlockType = repository.findAllBlockType(name,pageRequest);
        if(savedBlockType == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return savedBlockType;
    }

    public void enableDisEnable (EnableDisEnableDto request,HttpServletRequest request1){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        BlockType savedBlockType  = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested block type id does not exist!"));
        savedBlockType.setIsActive(request.isActive());
        savedBlockType.setUpdatedBy(userCurrent.getId());

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Disable/Enable blockType by :" + userCurrent.getUsername() ,
                        AuditTrailFlag.UPDATE,
                        " Disable/Enable blockType Request for:" +  savedBlockType.getId()
                                + " " +  savedBlockType.getName(),1, Utility.getClientIp(request1));
        repository.save(savedBlockType);

    }


    public List<BlockType> getAll(Boolean isActive){
        List<BlockType> savedBlockType = repository.findByIsActive(isActive);
        return savedBlockType;

    }
}
