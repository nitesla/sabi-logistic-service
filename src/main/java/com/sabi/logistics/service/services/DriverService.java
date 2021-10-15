package com.sabi.logistics.service.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.CountryDto;
import com.sabi.logistics.core.dto.request.DriverDto;
import com.sabi.logistics.core.dto.response.CountryResponseDto;
import com.sabi.logistics.core.dto.response.DriverResponseDto;
import com.sabi.logistics.core.models.Country;
import com.sabi.logistics.core.models.Driver;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.CountryRepository;
import com.sabi.logistics.service.repositories.DriverRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class DriverService {


    private DriverRepository repository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;

    public DriverService(DriverRepository repository, ModelMapper mapper, ObjectMapper objectMapper,Validations validations) {
        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
    }



    public DriverResponseDto createDriver(DriverDto request) {
//        validations.validateCountry(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Driver driver = mapper.map(request,Driver.class);
        Driver exist = repository.findByName(request.getName());
        if(exist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Driver already exist");
        }
        driver.setCreatedBy(userCurrent.getId());
        driver.setActive(true);
        driver = repository.save(driver);
        log.debug("Create new Driver - {}"+ new Gson().toJson(driver));
        return mapper.map(driver, DriverResponseDto.class);
    }



    public DriverResponseDto updateDriver(DriverDto request) {
//        validations.validateCountry(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Driver driver = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Driver Id does not exist!"));
        mapper.map(request, driver);
        driver.setUpdatedBy(userCurrent.getId());
        repository.save(driver);
        log.debug("Driver record updated - {}"+ new Gson().toJson(driver));
        return mapper.map(driver, DriverResponseDto.class);
    }


    public DriverResponseDto findDriver(Long id){
        Driver driver  = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested driver id does not exist!"));
        return mapper.map(driver,DriverResponseDto.class);
    }


    public Page<Driver> findAll(String name, PageRequest pageRequest ){
        Page<Driver> drivers = repository.findDrivers(name,pageRequest);
        if(drivers == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return drivers;

    }



    public void enableDisEnableState (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Driver driver  = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested driver Id does not exist!"));
        driver.setActive(request.isActive());
        driver.setUpdatedBy(userCurrent.getId());
        repository.save(driver);

    }


    public List<Driver> getAll(Boolean isActive){
        List<Driver> drivers = repository.findByIsActive(isActive);
        return drivers;

    }
}
