package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.BrandRequestDto;
import com.sabi.logistics.core.dto.response.BrandResponseDto;
import com.sabi.logistics.core.models.Brand;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.BrandRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class BrandService {

    private final BrandRepository brandRepository;
    private final ModelMapper mapper;
    private final Validations validations;

    public BrandService(BrandRepository brandRepository, ModelMapper mapper, ObjectMapper objectMapper, Validations validations) {
        this.brandRepository = brandRepository;
        this.mapper = mapper;
        this.validations = validations;
    }

    public BrandResponseDto createBrand(BrandRequestDto request) {
        validations.validateBrand(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Brand brand = mapper.map(request,Brand.class);
        Brand brandExists = brandRepository.findByName(request.getName());
        if(brandExists !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Brand already exist");
        }
        brand.setCreatedBy(userCurrent.getId());
        brand.setActive(true);
        brand = brandRepository.save(brand);
        log.debug("Create new brand - {}"+ new Gson().toJson(brand));
        return mapper.map(brand, BrandResponseDto.class);
    }

    public BrandResponseDto updateBrand(BrandRequestDto request) {
        validations.validateBrand(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Brand brand = brandRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested brand Id does not exist!"));
        mapper.map(request, brand);
        brand.setUpdatedBy(userCurrent.getId());
        brandRepository.save(brand);
        log.debug("brand record updated - {}"+ new Gson().toJson(brand));
        return mapper.map(brand, BrandResponseDto.class);
    }

    public BrandResponseDto findBrand(Long id){
        Brand brand  = brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested brand Id does not exist!"));
        return mapper.map(brand, BrandResponseDto.class);
    }


    public Page<Brand> findAll(String name, PageRequest pageRequest ){
        Page<Brand> brands = brandRepository.findBrand(name,pageRequest);
        if(brands == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return brands;

    }



    public void enableDisEnableState (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Brand brand  = brandRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested brand Id does not exist!"));
        brand.setActive(request.isActive());
        brand.setUpdatedBy(userCurrent.getId());
        brandRepository.save(brand);

    }


    public List<Brand> getAll(Boolean isActive){
        List<Brand> brands = brandRepository.findByIsActive(isActive);
        return brands;

    }
}
