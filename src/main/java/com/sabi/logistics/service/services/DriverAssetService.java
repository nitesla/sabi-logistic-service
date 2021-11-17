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
import com.sabi.logistics.core.dto.response.DriverAssetResponseDto;
import com.sabi.logistics.core.models.DriverAsset;
import com.sabi.logistics.core.models.PartnerAsset;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.DriverAssetRepository;
import com.sabi.logistics.service.repositories.DriverRepository;
import com.sabi.logistics.service.repositories.PartnerAssetRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class DriverAssetService {

    private DriverAssetRepository repository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;

    @Autowired
    private PartnerAssetRepository partnerAssetRepository;

    @Autowired
    private DriverRepository driverRepository;

    public DriverAssetService(DriverAssetRepository repository, ModelMapper mapper, ObjectMapper objectMapper,Validations validations) {
        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
    }


    public DriverAssetResponseDto createDriverAsset(DriverAssetDto request) {
        validations.validateDriverAsset(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DriverAsset driverAsset = mapper.map(request,DriverAsset.class);
        DriverAsset exist = repository.findByName(request.getName());
        if(exist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Driver asset already exist");
        }

        PartnerAsset partnerAsset = partnerAssetRepository.getOne(request.getPartnerAssetId());
        driverAsset.setPartnerName(partnerAsset.getPartnerName());
        driverAsset.setPartnerAssetName(partnerAsset.getName());

//        Driver driver = driverRepository.getOne(request.getDriverId());
//        driverAsset.setDriverName(driver.getName());

        driverAsset.setCreatedBy(userCurrent.getId());
        driverAsset.setIsActive(true);
        driverAsset = repository.save(driverAsset);
        log.debug("Create new Driver asset - {}"+ new Gson().toJson(driverAsset));
        return mapper.map(driverAsset, DriverAssetResponseDto.class);
    }


    public DriverAssetResponseDto updateDriverAsset(DriverAssetDto request) {
        validations.validateDriverAsset(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DriverAsset driverAsset = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Driver asset id does not exist!"));
        mapper.map(request, driverAsset);

        if(request.getPartnerAssetId() != null ) {
            PartnerAsset partnerAsset = partnerAssetRepository.getOne(request.getPartnerAssetId());
            driverAsset.setPartnerName(partnerAsset.getPartnerName());
            driverAsset.setPartnerAssetName(partnerAsset.getName());
        }

//        if(request.getDriverId() != null ) {
//            Driver driver = driverRepository.getOne(request.getDriverId());
//            driverAsset.setDriverName(driver.getName());
//        }

        driverAsset.setUpdatedBy(userCurrent.getId());
        repository.save(driverAsset);
        log.debug("Driver asset record updated - {}"+ new Gson().toJson(driverAsset));
        return mapper.map(driverAsset, DriverAssetResponseDto.class);
    }



    public DriverAssetResponseDto findDriverAsset(Long id){
        DriverAsset driverAsset  = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested driver asset id does not exist!"));
        return mapper.map(driverAsset,DriverAssetResponseDto.class);
    }



    public Page<DriverAsset> findAll(String name, Long driverId, Long partnerId, Long partnerAssetTypeId, PageRequest pageRequest ){
        Page<DriverAsset> drivers = repository.findDriverAssets(name, driverId, partnerId, partnerAssetTypeId,pageRequest);
        if(drivers == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return drivers;
    }


    public void enableDisEnableState (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DriverAsset driverAsset  = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested driver asset id does not exist!"));
        driverAsset.setIsActive(request.isActive());
        driverAsset.setUpdatedBy(userCurrent.getId());
        repository.save(driverAsset);

    }



    public List<DriverAsset> getAll(Boolean isActive){
        List<DriverAsset> drivers = repository.findByIsActive(isActive);
        return drivers;

    }
}
