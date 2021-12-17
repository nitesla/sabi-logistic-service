package com.sabi.logistics.service.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.DriverAssetDto;
import com.sabi.logistics.core.dto.request.PartnerAssetPictureDto;
import com.sabi.logistics.core.dto.response.DriverAssetResponseDto;
import com.sabi.logistics.core.dto.response.PartnerAssetPictureResponseDto;
import com.sabi.logistics.core.models.DriverAsset;
import com.sabi.logistics.core.models.PartnerAssetPicture;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.DriverAssetRepository;
import com.sabi.logistics.service.repositories.PartnerAssetPictureRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PartnerAssetPictureService {

    private PartnerAssetPictureRepository repository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;

    public PartnerAssetPictureService(PartnerAssetPictureRepository repository, ModelMapper mapper,
                                      ObjectMapper objectMapper,Validations validations) {
        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
    }


    public PartnerAssetPictureResponseDto createPartnerPicture(PartnerAssetPictureDto request) {
        validations.validatePartnerPicture(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerAssetPicture partnerAssetPicture = mapper.map(request,PartnerAssetPicture.class);
//        PartnerAssetPicture exist = repository.findByPartnerAssetIdAndPictureType(request.getPartnerAssetId(),request.getPictureType());
//        if(exist !=null){
//            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Asset picture already exist");
//        }
        partnerAssetPicture.setCreatedBy(userCurrent.getId());
        partnerAssetPicture.setIsActive(true);
        partnerAssetPicture = repository.save(partnerAssetPicture);
        log.debug("Create new asset picture - {}"+ new Gson().toJson(partnerAssetPicture));
        return mapper.map(partnerAssetPicture, PartnerAssetPictureResponseDto.class);
    }


    public  List<PartnerAssetPictureResponseDto> createPartnerPictures(List<PartnerAssetPictureDto> requests) {
//        validations.validatePartnerPicture(requests.);
        List<PartnerAssetPictureResponseDto> responseDtos = new ArrayList<>();
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        requests.forEach(request->{
            validations.validatePartnerPicture(request);
            PartnerAssetPicture partnerAssetPicture = mapper.map(request,PartnerAssetPicture.class);
//            PartnerAssetPicture exist = repository.findByPartnerAssetIdAndPictureType(request.getPartnerAssetId(),request.getPictureType());
//            if(exist !=null){
//                throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Asset picture already exist");
//            }
            partnerAssetPicture.setCreatedBy(userCurrent.getId());
            partnerAssetPicture.setIsActive(true);
            partnerAssetPicture = repository.save(partnerAssetPicture);
            log.debug("Create new asset picture - {}"+ new Gson().toJson(partnerAssetPicture));
             responseDtos.add(mapper.map(partnerAssetPicture, PartnerAssetPictureResponseDto.class));
        });
        return responseDtos;
    }

    public PartnerAssetPictureResponseDto updatePartnerPicture(PartnerAssetPictureDto request) {
        validations.validatePartnerPicture(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerAssetPicture assetPicture = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested asset picture id does not exist!"));
        mapper.map(request, assetPicture);
        assetPicture.setUpdatedBy(userCurrent.getId());
        repository.save(assetPicture);
        log.debug("Asset picture record updated - {}"+ new Gson().toJson(assetPicture));
        return mapper.map(assetPicture, PartnerAssetPictureResponseDto.class);
    }



    public PartnerAssetPictureResponseDto findPartnerPicture(Long id){
        PartnerAssetPicture assetPicture  = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested asset picture id does not exist!"));
        return mapper.map(assetPicture,PartnerAssetPictureResponseDto.class);
    }



    public Page<PartnerAssetPicture> findAll(Long partnerAssetId,String pictureType, PageRequest pageRequest ){
        Page<PartnerAssetPicture> assetPicture = repository.findAssetPicture(partnerAssetId,pictureType,pageRequest);
        if(assetPicture == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return assetPicture;
    }


    public void enableDisEnableState (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerAssetPicture assetPicture = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "asset picture id does not exist!"));
        assetPicture.setIsActive(request.isActive());
        assetPicture.setUpdatedBy(userCurrent.getId());
        repository.save(assetPicture);

    }



    public List<PartnerAssetPicture> getAll(Boolean isActive){
        List<PartnerAssetPicture> assetPictures = repository.findByIsActive(isActive);
        return assetPictures;

    }

}
