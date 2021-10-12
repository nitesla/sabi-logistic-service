package com.sabilogistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabilogistics.service.repositories.BlockTypeRepository;
import com.sabilogisticscore.dto.request.BlockTypeDto;
import com.sabilogisticscore.dto.response.BlockTypeResponseDto;
import com.sabilogisticscore.models.BlockType;
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

    public BlockTypeService(BlockTypeRepository repository, ModelMapper mapper, ObjectMapper objectMapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public BlockTypeResponseDto createBlockType(BlockTypeDto request) {
//        validations.validateCountry(request);
        BlockType partnerCategories = mapper.map(request,BlockType.class);
        BlockType exist = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested block type id does not exist!"));
        if(exist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " block type already exist");
        }
        partnerCategories.setCreatedBy(0l);
        partnerCategories.setActive(true);
        partnerCategories = repository.save(partnerCategories);
        log.debug("Create new block type - {}"+ new Gson().toJson(partnerCategories));
        return mapper.map(partnerCategories, BlockTypeResponseDto.class);
    }

    public BlockTypeResponseDto updateBlockType(BlockTypeDto request) {
//        validations.validateCountry(request);
        BlockType savedBlockType = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested block type id does not exist!"));
        mapper.map(request, savedBlockType);
        savedBlockType.setUpdatedBy(0l);
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
            new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested block type does not exist!");
        }
        return mapper.map(savedBlockType,BlockTypeResponseDto.class);
    }

    public Page<BlockType> findAll(String name, double length, double width, double heigth, double price,  PageRequest pageRequest ){
        Page<BlockType> savedBlockType = repository.findAllBlockType(name, length, width,heigth,price,pageRequest);
        if(savedBlockType == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return savedBlockType;
    }

    public void enableDisEnable (EnableDisEnableDto request){
        BlockType savedBlockType  = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested block type id does not exist!"));
        savedBlockType.setActive(request.isActive());
        savedBlockType.setUpdatedBy(0l);
        repository.save(savedBlockType);

    }


    public List<BlockType> getAll(Boolean isActive){
        List<BlockType> savedBlockType = repository.findByIsActive(isActive);
        return savedBlockType;

    }
}
