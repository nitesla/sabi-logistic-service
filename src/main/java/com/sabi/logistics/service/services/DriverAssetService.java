package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.service.AuditTrailService;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.AuditTrailFlag;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.framework.utils.Utility;
import com.sabi.logistics.core.dto.request.DriverAssetDto;
import com.sabi.logistics.core.dto.response.DriverAssetResponseDto;
import com.sabi.logistics.core.models.Driver;
import com.sabi.logistics.core.models.DriverAsset;
import com.sabi.logistics.core.models.PartnerAsset;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.DriverAssetRepository;
import com.sabi.logistics.service.repositories.DriverRepository;
import com.sabi.logistics.service.repositories.PartnerAssetRepository;
import com.sabi.logistics.service.repositories.PartnerAssetTypeRepository;
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
public class DriverAssetService {

    private DriverAssetRepository repository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;
    private final AuditTrailService auditTrailService;

    @Autowired
    private PartnerAssetRepository partnerAssetRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PartnerAssetTypeRepository partnerAssetTypeRepository;


    public DriverAssetService(DriverAssetRepository repository, ModelMapper mapper, ObjectMapper objectMapper,Validations validations,
                              AuditTrailService auditTrailService) {
        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
        this.auditTrailService = auditTrailService;
    }


    public DriverAssetResponseDto createDriverAsset(DriverAssetDto request,HttpServletRequest request1) {
        validations.validateDriverAsset(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DriverAsset driverAsset = mapper.map(request,DriverAsset.class);
        DriverAsset exist = repository.findByDriverIdAndPartnerAssetId(request.getDriverId(),request.getPartnerAssetId());
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

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Create new driver asset by :" + userCurrent.getUsername(),
                        AuditTrailFlag.CREATE,
                        " Create new driver asset for:" + driverAsset.getDriverName() + " "+ driverAsset.getAssetType() ,1, Utility.getClientIp(request1));
        return mapper.map(driverAsset, DriverAssetResponseDto.class);
    }


    public DriverAssetResponseDto updateDriverAsset(DriverAssetDto request,HttpServletRequest request1) {
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
        driverAsset.setUpdatedBy(userCurrent.getId());
        repository.save(driverAsset);
        log.debug("Driver asset record updated - {}"+ new Gson().toJson(driverAsset));

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Update driver asset by username:" + userCurrent.getUsername(),
                        AuditTrailFlag.UPDATE,
                        " Update driver asset Request for:" + driverAsset.getId() ,1, Utility.getClientIp(request1));
        return mapper.map(driverAsset, DriverAssetResponseDto.class);
    }



    public DriverAssetResponseDto findDriverAsset(Long id){
        DriverAsset driverAsset  = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested driver asset id does not exist!"));
        User savedUser = userRepository.findById(driverAsset.getDriverId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested driver id (user) does not exist!"));
        PartnerAsset partnerAsset = partnerAssetRepository.findById(driverAsset.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partner asset id does not exist!"));
        driverAsset.setFirstName(savedUser.getFirstName());
        driverAsset.setLastName(savedUser.getLastName());
        driverAsset.setEmail(savedUser.getEmail());
        driverAsset.setPhoneNumber(savedUser.getPhone());
        driverAsset.setAssetTypeName(partnerAsset.getName());
        driverAsset.setAssetType(partnerAsset.getName());
        return mapper.map(driverAsset,DriverAssetResponseDto.class);
    }

    public Page<DriverAsset> findAll(Long driverId, Long partnerAssestId, PageRequest pageRequest ){
        Page<DriverAsset> drivers = repository.findDriverAssets(driverId, partnerAssestId,pageRequest);
        if(drivers == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        drivers.getContent().forEach(driver ->{
            Driver nigga = driverRepository.findById(driver.getDriverId())
                    .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                            "Requested driver id (user) does not exist!"));
            if (nigga == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Driver Id");
            }
            User savedUser = userRepository.getOne(nigga.getUserId());
            log.info("Checking {} ::::::::::::::::::::::::::::::::::::::::::::" + savedUser);
            PartnerAsset partnerAsset = partnerAssetRepository.findById(driver.getPartnerAssetId())
                    .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                            "Requested partner asset id does not exist!"));
            driver.setFirstName(savedUser.getFirstName());
            driver.setLastName(savedUser.getLastName());
            driver.setEmail(savedUser.getEmail());
            driver.setPhoneNumber(savedUser.getPhone());
            driver.setAssetTypeName(partnerAsset.getAssetTypeName());
            driver.setPartnerAssetName(partnerAsset.getName());

        });

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
