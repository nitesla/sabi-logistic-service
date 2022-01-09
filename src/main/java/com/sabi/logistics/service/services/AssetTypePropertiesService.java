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
import com.sabi.logistics.core.dto.request.AssetTypePropertiesDto;
import com.sabi.logistics.core.dto.response.AssetTypePropertiesResponseDto;
import com.sabi.logistics.core.models.AssetTypeProperties;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.AssetTypePropertiesRepository;
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
public class AssetTypePropertiesService {


    @Autowired
    private AssetTypePropertiesRepository repository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;
    private final AuditTrailService auditTrailService;

    public AssetTypePropertiesService( ModelMapper mapper,
                                      ObjectMapper objectMapper,Validations validations,AuditTrailService auditTrailService) {
//        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
        this.auditTrailService = auditTrailService;
    }


    public AssetTypePropertiesResponseDto createAssetTypeProperties(AssetTypePropertiesDto request,HttpServletRequest request1) {
        validations.validateAssetTypeProperties(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        AssetTypeProperties assetTypeProperties = mapper.map(request,AssetTypeProperties.class);
        AssetTypeProperties exist = repository.findByName(request.getName());
        if(exist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Asset type already exist");
        }
        assetTypeProperties.setCreatedBy(userCurrent.getId());
        assetTypeProperties.setIsActive(true);
        assetTypeProperties = repository.save(assetTypeProperties);
        log.debug("Create new asset type - {}"+ new Gson().toJson(assetTypeProperties));

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Create new assetTypeProperties  by :" + userCurrent.getUsername(),
                        AuditTrailFlag.CREATE,
                        " Create new assetTypeProperties for:" + assetTypeProperties.getName() ,1, Utility.getClientIp(request1));
        return mapper.map(assetTypeProperties, AssetTypePropertiesResponseDto.class);
    }



    public AssetTypePropertiesResponseDto updateAssetTypeProperties(AssetTypePropertiesDto request,HttpServletRequest request1) {
        validations.validateAssetTypeProperties(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        AssetTypeProperties assetTypeProperties = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested asset type Id does not exist!"));
        mapper.map(request, assetTypeProperties);
        assetTypeProperties.setUpdatedBy(userCurrent.getId());
        repository.save(assetTypeProperties);
        log.debug("asset type record updated - {}"+ new Gson().toJson(assetTypeProperties));

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Update assetTypeProperties by username:" + userCurrent.getUsername(),
                        AuditTrailFlag.UPDATE,
                        " Update assetTypeProperties Request for:" + assetTypeProperties.getId(),1, Utility.getClientIp(request1));
        return mapper.map(assetTypeProperties, AssetTypePropertiesResponseDto.class);
    }


    public AssetTypePropertiesResponseDto findAsstType(Long id){
        AssetTypeProperties assetTypeProperties  = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested asset type Id does not exist!"));
        return mapper.map(assetTypeProperties,AssetTypePropertiesResponseDto.class);
    }



    public Page<AssetTypeProperties> findAll(String name, PageRequest pageRequest ){
        Page<AssetTypeProperties> assetTypeProperties = repository.findAssets(name,pageRequest);
        if(assetTypeProperties == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return assetTypeProperties;

    }



    public void enableDisEnable (EnableDisEnableDto request,HttpServletRequest request1){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        AssetTypeProperties assetTypeProperties  = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested asset type Id does not exist!"));
        assetTypeProperties.setIsActive(request.isActive());
        assetTypeProperties.setUpdatedBy(userCurrent.getId());

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Disable/Enable assetTypeProperties by :" + userCurrent.getUsername() ,
                        AuditTrailFlag.UPDATE,
                        " Disable/Enable assetTypeProperties Request for:" +  assetTypeProperties.getId()
                                + " " +  assetTypeProperties.getName(),1, Utility.getClientIp(request1));
        repository.save(assetTypeProperties);

    }


    public List<AssetTypeProperties> getAll(Boolean isActive){
        List<AssetTypeProperties> assetTypeProperties = repository.findByIsActive(isActive);
        return assetTypeProperties;

    }


}
