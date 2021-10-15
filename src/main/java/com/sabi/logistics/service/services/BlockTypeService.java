package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
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

import java.util.List;

@Service
@Slf4j
public class BlockTypeService {

    private BlockTypeRepository repository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;

    public BlockTypeService(BlockTypeRepository repository, ModelMapper mapper, ObjectMapper objectMapper, Validations validations) {
        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
    }

    public BlockTypeResponseDto createBlockType(BlockTypeDto request) {
        validations.validateBlockType(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        BlockType partnerCategories = mapper.map(request,BlockType.class);
        BlockType exist = repository.findByName(request.getName());
        if(exist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " block type already exist");
        }
        partnerCategories.setCreatedBy(userCurrent.getId());
        partnerCategories.setActive(true);
        partnerCategories = repository.save(partnerCategories);
        log.debug("Create new block type - {}"+ new Gson().toJson(partnerCategories));
        return mapper.map(partnerCategories, BlockTypeResponseDto.class);
    }

    public BlockTypeResponseDto updateBlockType(BlockTypeDto request) {
        validations.validateBlockType(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        BlockType savedBlockType = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested block type id does not exist!"));
        mapper.map(request, savedBlockType);
        savedBlockType.setUpdatedBy(userCurrent.getId());
        repository.save(savedBlockType);
        log.debug("block type record updated - {}"+ new Gson().toJson(savedBlockType));
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

    public void enableDisEnable (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        BlockType savedBlockType  = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested block type id does not exist!"));
        savedBlockType.setActive(request.isActive());
        savedBlockType.setUpdatedBy(userCurrent.getId());
        repository.save(savedBlockType);

    }


    public List<BlockType> getAll(Boolean isActive){
        List<BlockType> savedBlockType = repository.findByIsActive(isActive);
        return savedBlockType;

    }
}
