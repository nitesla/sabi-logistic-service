package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.PartnerLocationDto;
import com.sabi.logistics.core.dto.response.PartnerLocationResponseDto;
import com.sabi.logistics.core.models.PartnerLocation;
import com.sabi.logistics.service.repositories.PartnerLocationRepository;
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


    public PartnerLocationService(PartnerLocationRepository repository, ModelMapper mapper, ObjectMapper objectMapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public PartnerLocationResponseDto createPartnerLocation(PartnerLocationDto request) {
//        validations.validateCountry(request);
        PartnerLocation partnerProperties = mapper.map(request,PartnerLocation.class);
        PartnerLocation exist = repository.findPartnerLocationById(request.getId());
//                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
//                        "Requested partner location id does not exist!"));
        if (exist != null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Partner location already exist");
        }
        partnerProperties.setCreatedBy(0l);
        partnerProperties.setActive(true);
        partnerProperties = repository.save(partnerProperties);
//        log.debug("Create new partner location - {}"+ new Gson().toJson(partnerProperties));
        return mapper.map(partnerProperties, PartnerLocationResponseDto.class);
    }


    public PartnerLocationResponseDto updatePartnerLocation(PartnerLocationDto request) {
//        validations.validateCountry(request);
        PartnerLocation partnerProperties = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partner location Id does not exist!"));
        mapper.map(request, partnerProperties);
        partnerProperties.setUpdatedBy(0l);
        repository.save(partnerProperties);
        log.debug("partner location record updated - {}"+ new Gson().toJson(partnerProperties));
        return mapper.map(partnerProperties, PartnerLocationResponseDto.class);
    }

    public PartnerLocationResponseDto findByPartnerLocationId(Long partnerId){
        PartnerLocation savedPartnerCategories  = repository.findPartnerLocationById(partnerId);
        if (savedPartnerCategories == null){
            new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
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
        PartnerLocation savedPartnerCategories  = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partner location id does not exist!"));
        savedPartnerCategories.setActive(request.isActive());
        savedPartnerCategories.setUpdatedBy(0l);
        repository.save(savedPartnerCategories);

    }


    public List<PartnerLocation> getAll(Boolean isActive){
        List<PartnerLocation> partnerCategories = repository.findByIsActive(isActive);
        return partnerCategories;

    }

}
