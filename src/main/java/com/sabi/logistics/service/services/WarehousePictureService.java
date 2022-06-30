package com.sabi.logistics.service.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.WarehousePictureDto;
import com.sabi.logistics.core.dto.response.WarehousePictureResponseDto;
import com.sabi.logistics.core.models.WarehousePicture;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.WarehousePictureRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class WarehousePictureService {

    private WarehousePictureRepository repository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;

    public WarehousePictureService(WarehousePictureRepository repository, ModelMapper mapper,
                                   ObjectMapper objectMapper, Validations validations) {
        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
    }


    public WarehousePictureResponseDto createWarehousePicture(WarehousePictureDto request) {
        validations.validateWarehousePicture(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        WarehousePicture warehousePicture = mapper.map(request,WarehousePicture.class);
        warehousePicture.setCreatedBy(userCurrent.getId());
        warehousePicture.setIsActive(true);
        warehousePicture = repository.save(warehousePicture);
        log.debug("Create new asset picture - {}"+ new Gson().toJson(warehousePicture));
        return mapper.map(warehousePicture, WarehousePictureResponseDto.class);
    }


    public  List<WarehousePictureResponseDto> createWarehousePictures(List<WarehousePictureDto> requests) {
        List<WarehousePictureResponseDto> responseDtos = new ArrayList<>();
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        requests.forEach(request->{
            validations.validateWarehousePicture(request);
            WarehousePicture warehousePicture = mapper.map(request,WarehousePicture.class);
            warehousePicture.setCreatedBy(userCurrent.getId());
            warehousePicture.setIsActive(true);
            warehousePicture = repository.save(warehousePicture);
            log.debug("Create new asset picture - {}"+ new Gson().toJson(warehousePicture));
             responseDtos.add(mapper.map(warehousePicture, WarehousePictureResponseDto.class));
        });
        return responseDtos;
    }

    public WarehousePictureResponseDto updateWarehousePicture(WarehousePictureDto request) {
        validations.validateWarehousePicture(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        WarehousePicture assetPicture = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested asset picture id does not exist!"));
        mapper.map(request, assetPicture);
        assetPicture.setUpdatedBy(userCurrent.getId());
        repository.save(assetPicture);
        log.debug("Asset picture record updated - {}"+ new Gson().toJson(assetPicture));
        return mapper.map(assetPicture, WarehousePictureResponseDto.class);
    }



    public WarehousePictureResponseDto findWarehousePicture(Long id){
        WarehousePicture assetPicture  = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested asset picture id does not exist!"));
        return mapper.map(assetPicture,WarehousePictureResponseDto.class);
    }



    public Page<WarehousePicture> findAll(Long warehouseId, PageRequest pageRequest ){
        Page<WarehousePicture> assetPicture = repository.findWarehousePicture(warehouseId,pageRequest);
        if(assetPicture == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return assetPicture;
    }


    public void enableDisEnableState (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        WarehousePicture assetPicture = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "asset picture id does not exist!"));
        assetPicture.setIsActive(request.getIsActive());
        assetPicture.setUpdatedBy(userCurrent.getId());
        repository.save(assetPicture);

    }



    public List<WarehousePicture> getAll(Boolean isActive){
        List<WarehousePicture> assetPictures = repository.findByIsActive(isActive);
        return assetPictures;

    }

}
