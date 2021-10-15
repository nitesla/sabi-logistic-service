package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.PartnerLocationDto;
import com.sabi.logistics.core.dto.response.PartnerLocationResponseDto;
import com.sabi.logistics.core.models.PartnerLocation;
import com.sabi.logistics.core.models.PartnerProperties;
import com.sabi.logistics.core.models.State;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.PartnerLocationRepository;
import com.sabi.logistics.service.repositories.PartnerPropertiesRepository;
import com.sabi.logistics.service.repositories.StateRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@Service
public class PartnerLocationService {

    private PartnerLocationRepository repository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;

    public PartnerLocationService(PartnerLocationRepository repository, ModelMapper mapper, ObjectMapper objectMapper, Validations validations) {
        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
    }

    public PartnerLocationResponseDto createPartnerLocation(PartnerLocationDto request) {
        validations.validatePartnerLocation(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerLocation partnerProperties = mapper.map(request,PartnerLocation.class);
        PartnerLocation exist = repository.findPartnerLocationById(request.getId());
        if (exist != null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Partner location already exist");
        }
        partnerProperties.setCreatedBy(userCurrent.getId());
        partnerProperties.setActive(true);
        partnerProperties = repository.save(partnerProperties);
        return mapper.map(partnerProperties, PartnerLocationResponseDto.class);
    }


    public PartnerLocationResponseDto updatePartnerLocation(PartnerLocationDto request) {
        validations.validatePartnerLocation(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerLocation partnerProperties = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partner location Id does not exist!"));
        mapper.map(request, partnerProperties);
        partnerProperties.setUpdatedBy(userCurrent.getId());
        repository.save(partnerProperties);
        log.debug("partner location record updated - {}"+ new Gson().toJson(partnerProperties));
        return mapper.map(partnerProperties, PartnerLocationResponseDto.class);
    }

    public PartnerLocationResponseDto findByPartnerLocationId(Long partnerId){
        PartnerLocation savedPartnerCategories  = repository.findPartnerLocationById(partnerId);
        if (savedPartnerCategories == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested partner location does not exist!");
        }
        return mapper.map(savedPartnerCategories,PartnerLocationResponseDto.class);
    }

    public Page<PartnerLocation> findAll(Long id, PageRequest pageRequest ){
        Page<PartnerLocation> savedPartnerCategories = repository.findPartnerLocation(id,pageRequest);
        if(savedPartnerCategories == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return savedPartnerCategories;
    }

    public void enableDisEnable (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerLocation savedPartnerCategories  = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partner location id does not exist!"));
        savedPartnerCategories.setActive(request.isActive());
        savedPartnerCategories.setUpdatedBy(userCurrent.getId());
        repository.save(savedPartnerCategories);

    }


    public List<PartnerLocation> getAll(Boolean isActive){
        List<PartnerLocation> partnerCategories = repository.findByIsActive(isActive);
        return partnerCategories;

    }

}
