package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.PartnerCategoriesDto;
import com.sabi.logistics.core.dto.response.PartnerCategoriesResponseDto;
import com.sabi.logistics.core.models.Category;
import com.sabi.logistics.core.models.PartnerCategories;
import com.sabi.logistics.core.models.PartnerProperties;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.CategoryRepository;
import com.sabi.logistics.service.repositories.PartnerCategoriesRepository;
import com.sabi.logistics.service.repositories.PartnerPropertiesRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PartnerCategoriesService {

    private PartnerCategoriesRepository repository;
    private CategoryRepository categoryRepository;
    @Autowired
    private PartnerPropertiesRepository partnerPropertiesRepository;
    private final ModelMapper mapper;
    private final Validations validations;


    public PartnerCategoriesService(PartnerCategoriesRepository repository, ModelMapper mapper, ObjectMapper objectMapper, CategoryRepository categoryRepository, Validations validations) {
        this.repository = repository;
        this.mapper = mapper;
        this.categoryRepository = categoryRepository;
        this.validations = validations;
//        this.objectMapper = objectMapper;
    }


    public PartnerCategoriesResponseDto createPartnerCategory(PartnerCategoriesDto request) {
//        validations.validatePartnerCategories(request);
        PartnerCategories partnerCategories = mapper.map(request,PartnerCategories.class);
        PartnerCategories exist = repository.findPartnerCategoriesById(request.getId());
        Category savedCategory = categoryRepository.findCategoriesById(request.getCategoryId());
        PartnerProperties savePartner = partnerPropertiesRepository.findPartnerPropertiesById(request.getPartnerId());

        if(exist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Partner Category already exist");
        }
        if (savedCategory == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }

        if (savePartner == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
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

    public PartnerProperties findByCategoryId(Long id){
        PartnerProperties savedPartnerCategories  = partnerPropertiesRepository.findPartnerPropertiesById(id);
                if (savedPartnerCategories == null){
        new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partner Category does not exist!");
                }
        return mapper.map(savedPartnerCategories,PartnerProperties.class);
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
