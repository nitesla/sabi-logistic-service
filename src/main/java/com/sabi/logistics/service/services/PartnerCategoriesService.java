package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.dto.responseDto.PartnersCategoryReturn;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.PartnerCategoriesDto;
import com.sabi.logistics.core.dto.response.PartnerCategoriesResponseDto;
import com.sabi.logistics.core.models.Partner;
import com.sabi.logistics.core.models.PartnerCategories;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.PartnerCategoriesRepository;
import com.sabi.logistics.service.repositories.PartnerRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PartnerCategoriesService {

    private PartnerCategoriesRepository repository;
    @Autowired
    private PartnerRepository partnerRepository;
    private final ModelMapper mapper;
    private final Validations validations;


    public PartnerCategoriesService(PartnerCategoriesRepository repository, ModelMapper mapper, ObjectMapper objectMapper, Validations validations) {
        this.repository = repository;
        this.mapper = mapper;
        this.validations = validations;
    }


    public PartnerCategoriesResponseDto createPartnerCategory(PartnerCategoriesDto request) {
        validations.validatePartnerCategories(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerCategories partnerCategories = mapper.map(request,PartnerCategories.class);
        PartnerCategories exist = repository.findPartnerCategoriesById(request.getId());

        if(exist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Partner Category already exist");
        }
        partnerCategories.setCreatedBy(userCurrent.getId());
        partnerCategories.setIsActive(true);
        partnerCategories = repository.save(partnerCategories);
        log.debug("Create new partner Category - {}"+ new Gson().toJson(partnerCategories));
        return mapper.map(partnerCategories, PartnerCategoriesResponseDto.class);
    }

    public PartnerCategoriesResponseDto updatePartnerCategory(PartnerCategoriesDto request) {
        validations.validatePartnerCategories(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerCategories partnerCategories = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partner category id does not exist!"));
        mapper.map(request, partnerCategories);
        partnerCategories.setUpdatedBy(userCurrent.getId());
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

    public Partner findByCategoryId(Long id){
        Partner savedPartnerCategories  = partnerRepository.findPartnerPropertiesById(id);
                if (savedPartnerCategories == null){
        throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partner Category does not exist!");
                }
        return mapper.map(savedPartnerCategories,Partner.class);
    }

    public Page<PartnerCategories> findAll(Long partnerId, Long categoryId, PageRequest pageRequest ){
        Page<PartnerCategories> savedPartnerCategories = repository.findPartnerCategories(partnerId,categoryId,pageRequest);
        if(savedPartnerCategories == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return savedPartnerCategories;
    }

    public void enableDisEnable (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerCategories savedPartnerCategories  = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partner category id does not exist!"));
        savedPartnerCategories.setIsActive(request.isActive());
        savedPartnerCategories.setUpdatedBy(userCurrent.getId());
        repository.save(savedPartnerCategories);

    }


    public List<PartnerCategories> getAll(Boolean isActive){
        List<PartnerCategories> partnerCategories = repository.findByIsActive(isActive);
        return partnerCategories;

    }



    public List<PartnersCategoryReturn> partnerCategoryReturn(Long partnerId) {
        List<PartnersCategoryReturn> resultLists = new ArrayList<>();
        List<Object[]> result = repository.findAllByPartnerId(partnerId);
        try {
            result.forEach(r -> {
                PartnersCategoryReturn partnersCategoryReturn = new PartnersCategoryReturn();
                partnersCategoryReturn.setCategoryId((Long) r[0]);
                resultLists.add(partnersCategoryReturn);
            });
        } catch (Exception e) {
            log.info("Error in returning object list" +e);
        }
        return resultLists;
    }
}
