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
import com.sabi.logistics.core.dto.response.CountryResponseDto;
import com.sabi.logistics.core.models.Country;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.CountryRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class CountryService {


    private CountryRepository countryRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;

    public CountryService(CountryRepository countryRepository, ModelMapper mapper, ObjectMapper objectMapper,Validations validations) {
        this.countryRepository = countryRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
    }



    /** <summary>
     * Country creation
     * </summary>
     * <remarks>this method is responsible for creation of new country</remarks>
     */

    public CountryResponseDto createCountry(CountryDto request) {
        validations.validateCountry(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Country country = mapper.map(request,Country.class);
        Country countryExist = countryRepository.findByName(request.getName());
        if(countryExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Country already exist");
        }
        country.setCreatedBy(userCurrent.getId());
        country.setIsActive(true);
        country = countryRepository.save(country);
        log.debug("Create new Country - {}"+ new Gson().toJson(country));
        return mapper.map(country, CountryResponseDto.class);
    }



    /** <summary>
     * Country update
     * </summary>
     * <remarks>this method is responsible for updating already existing country</remarks>
     */

    public CountryResponseDto updateCountry(CountryDto request) {
        validations.validateCountry(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Country country = countryRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Country Id does not exist!"));
        mapper.map(request, country);
        country.setUpdatedBy(userCurrent.getId());
        countryRepository.save(country);
        log.debug("Country record updated - {}"+ new Gson().toJson(country));
        return mapper.map(country, CountryResponseDto.class);
    }




    /** <summary>
     * Find Country
     * </summary>
     * <remarks>this method is responsible for getting a single record</remarks>
     */
    public CountryResponseDto findCountry(Long id){
        Country country  = countryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Country Id does not exist!"));
        return mapper.map(country,CountryResponseDto.class);
    }



    /** <summary>
     * Find all Country
     * </summary>
     * <remarks>this method is responsible for getting all records in pagination</remarks>
     */
    public Page<Country> findAll(String name,String code, PageRequest pageRequest ){
        Page<Country> country = countryRepository.findCountries(name,code,pageRequest);
        if(country == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return country;

    }


    /** <summary>
     * Enable disenable
     * </summary>
     * <remarks>this method is responsible for enabling and dis enabling a country</remarks>
     */
    public void enableDisEnableState (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Country country = countryRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Country Id does not exist!"));
        country.setIsActive(request.isActive());
        country.setUpdatedBy(userCurrent.getId());
        countryRepository.save(country);

    }


    public List<Country> getAll(String name, String code){
        List<Country> countries = countryRepository.findAllByNameAndCode(name,code);
        return countries;

    }
}
