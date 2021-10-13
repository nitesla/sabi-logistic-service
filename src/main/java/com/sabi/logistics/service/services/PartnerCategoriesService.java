package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.PartnerCategoriesDto;
import com.sabi.logistics.core.dto.response.PartnerCategoriesResponseDto;
import com.sabi.logistics.core.models.PartnerCategories;
import com.sabi.logistics.service.repositories.PartnerCategoriesRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PartnerCategoriesService {

    private PartnerCategoriesRepository repository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;

    public PartnerCategoriesService(PartnerCategoriesRepository repository, ModelMapper mapper, ObjectMapper objectMapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }


    public PartnerCategoriesResponseDto createPartnerCategory(PartnerCategoriesDto request) {
//        validations.validateCountry(request);
        PartnerCategories partnerCategories = mapper.map(request,PartnerCategories.class);
        PartnerCategories exist = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partner category id does not exist!"));
        if(exist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Partner Category already exist");
        }
        partnerCategories.setCreatedBy(0l);
        partnerCategories.setActive(true);
        partnerCategories = repository.save(partnerCategories);
        log.debug("Create new partner Category - {}"+ new Gson().toJson(partnerCategories));
        return mapper.map(partnerCategories, PartnerCategoriesResponseDto.class);
    }

    public PartnerCategoriesResponseDto updatePartnerCategory(PartnerCategoriesDto request) {
//        validations.validateCountry(request);
        PartnerCategories partnerCategories = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partner category id does not exist!"));
        mapper.map(request, partnerCategories);
        partnerCategories.setUpdatedBy(0l);
        repository.save(partnerCategories);
        log.debug("partner category record updated - {}"+ new Gson().toJson(partnerCategories));
        return mapper.map(partnerCategories, PartnerCategoriesResponseDto.class);
    }

    public PartnerCategoriesResponseDto findById(Long id){
        PartnerCategories partnerCategories  = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partner category id does not exist!"));
        return mapper.map(partnerCategories,PartnerCategoriesResponseDto.class);
    }

//    public PartnerCategoriesResponseDto findByCategoryId(Long categoryId){
//        PartnerCategories savedPartnerCategories  = repository.findByCategoryIdId(categoryId);
//                if (savedPartnerCategories == null){
//        new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
//                        "Requested partner Category does not exist!");
//                }
//        return mapper.map(savedPartnerCategories,PartnerCategoriesResponseDto.class);
//    }

    public PartnerCategoriesResponseDto findByPartnerId(Long partnerId){
        PartnerCategories savedPartnerCategories  = repository.findByPartnerId(partnerId);
        if (savedPartnerCategories == null){
            new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested partner Category does not exist!");
        }
        return mapper.map(savedPartnerCategories,PartnerCategoriesResponseDto.class);
    }

    public Page<PartnerCategories> findAll(Long partnerId, Long categoryId, PageRequest pageRequest ){
        Page<PartnerCategories> savedPartnerCategories = repository.findPartnerCategories(partnerId,categoryId,pageRequest);
        if(savedPartnerCategories == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return savedPartnerCategories;
    }

    public void enableDisEnable (EnableDisEnableDto request){
        PartnerCategories savedPartnerCategories  = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partner category id does not exist!"));
        savedPartnerCategories.setActive(request.isActive());
        savedPartnerCategories.setUpdatedBy(0l);
        repository.save(savedPartnerCategories);

    }


    public List<PartnerCategories> getAll(Boolean isActive){
        List<PartnerCategories> partnerCategories = repository.findByIsActive(isActive);
        return partnerCategories;

    }

}
