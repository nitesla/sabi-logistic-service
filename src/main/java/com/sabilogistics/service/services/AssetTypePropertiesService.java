package com.sabilogistics.service.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabilogistics.service.helper.Validations;
import com.sabilogistics.service.repositories.AssetTypePropertiesRepository;
import com.sabilogistics.service.repositories.CountryRepository;
import com.sabilogisticscore.dto.request.AssetTypePropertiesDto;
import com.sabilogisticscore.dto.request.CountryDto;
import com.sabilogisticscore.dto.response.AssetTypePropertiesResponseDto;
import com.sabilogisticscore.dto.response.CountryResponseDto;
import com.sabilogisticscore.models.AssetTypeProperties;
import com.sabilogisticscore.models.Country;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AssetTypePropertiesService {


    private AssetTypePropertiesRepository repository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;

    public AssetTypePropertiesService(AssetTypePropertiesRepository repository, ModelMapper mapper,
                                      ObjectMapper objectMapper,Validations validations) {
        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
    }


    public AssetTypePropertiesResponseDto createAssetTypeProperties(AssetTypePropertiesDto request) {
        validations.validateAssetTypeProperties(request);
        AssetTypeProperties assetTypeProperties = mapper.map(request,AssetTypeProperties.class);
        AssetTypeProperties exist = repository.findByName(request.getName());
        if(exist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Asset type already exist");
        }
        assetTypeProperties.setCreatedBy(0l);
        assetTypeProperties.setActive(true);
        assetTypeProperties = repository.save(assetTypeProperties);
        log.debug("Create new asset type - {}"+ new Gson().toJson(assetTypeProperties));
        return mapper.map(assetTypeProperties, AssetTypePropertiesResponseDto.class);
    }



    public AssetTypePropertiesResponseDto updateAssetTypeProperties(AssetTypePropertiesDto request) {
        validations.validateAssetTypeProperties(request);
        AssetTypeProperties assetTypeProperties = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested asset type Id does not exist!"));
        mapper.map(request, assetTypeProperties);
        assetTypeProperties.setUpdatedBy(0l);
        repository.save(assetTypeProperties);
        log.debug("asset type record updated - {}"+ new Gson().toJson(assetTypeProperties));
        return mapper.map(assetTypeProperties, AssetTypePropertiesResponseDto.class);
    }


    public AssetTypePropertiesResponseDto findAsstType(Long id){
        AssetTypeProperties assetTypeProperties  = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested asset type Id does not exist!"));
        return mapper.map(assetTypeProperties,AssetTypePropertiesResponseDto.class);
    }



    public Page<AssetTypeProperties> findAll(String name,  PageRequest pageRequest ){
        Page<AssetTypeProperties> assetTypeProperties = repository.findAssets(name,pageRequest);
        if(assetTypeProperties == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return assetTypeProperties;

    }



    public void enableDisEnable (EnableDisEnableDto request){
        AssetTypeProperties assetTypeProperties  = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested asset type Id does not exist!"));
        assetTypeProperties.setActive(request.isActive());
        assetTypeProperties.setUpdatedBy(0l);
        repository.save(assetTypeProperties);

    }


    public List<AssetTypeProperties> getAll(Boolean isActive){
        List<AssetTypeProperties> assetTypeProperties = repository.findByIsActive(isActive);
        return assetTypeProperties;

    }


}
