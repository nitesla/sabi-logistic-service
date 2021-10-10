package com.sabilogistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabilogistics.service.repositories.AssetTypePropertiesRepository;
import com.sabilogistics.service.repositories.PartnerPropertiesRepository;
import com.sabilogisticscore.dto.request.AssetTypePropertiesDto;
import com.sabilogisticscore.dto.request.PartnerPropertiesDto;
import com.sabilogisticscore.dto.response.AssetTypePropertiesResponseDto;
import com.sabilogisticscore.dto.response.PartnerPropertiesResponseDto;
import com.sabilogisticscore.models.AssetTypeProperties;
import com.sabilogisticscore.models.PartnerProperties;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PartnerPropertiesService {

    private PartnerPropertiesRepository repository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
//    private final Validations validations;

    public PartnerPropertiesService(PartnerPropertiesRepository repository, ModelMapper mapper, ObjectMapper objectMapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
//        this.validations = validations;
    }


    public PartnerPropertiesResponseDto createPartnerProperties(PartnerPropertiesDto request) {
//        validations.validateCountry(request);
        PartnerProperties partnerProperties = mapper.map(request,PartnerProperties.class);
        PartnerProperties exist = repository.findByName(request.getName());
        if(exist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " partner properties already exist");
        }
        partnerProperties.setCreatedBy(0l);
        partnerProperties.setActive(true);
        partnerProperties = repository.save(partnerProperties);
        log.debug("Create new partner asset - {}"+ new Gson().toJson(partnerProperties));
        return mapper.map(partnerProperties, PartnerPropertiesResponseDto.class);
    }


    public PartnerPropertiesResponseDto updatePartnerProperties(PartnerPropertiesDto request) {
//        validations.validateCountry(request);
        PartnerProperties partnerProperties = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partner properties Id does not exist!"));
        mapper.map(request, partnerProperties);
        partnerProperties.setUpdatedBy(0l);
        repository.save(partnerProperties);
        log.debug("partner asset record updated - {}"+ new Gson().toJson(partnerProperties));
        return mapper.map(partnerProperties, PartnerPropertiesResponseDto.class);
    }


    public PartnerPropertiesResponseDto findPartnerAsset(Long id){
        PartnerProperties partnerProperties  = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partner properties Id does not exist!"));
        return mapper.map(partnerProperties,PartnerPropertiesResponseDto.class);
    }





    public Page<PartnerProperties> findAll(String name, PageRequest pageRequest ){
        Page<PartnerProperties> partnerProperties = repository.findPartnersProperties(name,pageRequest);
        if(partnerProperties == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return partnerProperties;

    }



    public void enableDisEnable (EnableDisEnableDto request){
        PartnerProperties partnerProperties = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partner properties Id does not exist!"));
        partnerProperties.setActive(request.isActive());
        partnerProperties.setUpdatedBy(0l);
        repository.save(partnerProperties);

    }


    public List<PartnerProperties> getAll(Boolean isActive){
        List<PartnerProperties> partnerProperties = repository.findByIsActive(isActive);
        return partnerProperties;

    }
}
