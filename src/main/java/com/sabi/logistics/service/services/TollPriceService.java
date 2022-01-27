package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.ProductRequestDto;
import com.sabi.logistics.core.dto.request.StateDto;
import com.sabi.logistics.core.dto.request.TollPricesDto;
import com.sabi.logistics.core.dto.response.ProductResponseDto;
import com.sabi.logistics.core.dto.response.StateResponseDto;
import com.sabi.logistics.core.dto.response.TollPricesResponseDto;
import com.sabi.logistics.core.models.Country;
import com.sabi.logistics.core.models.Product;
import com.sabi.logistics.core.models.State;
import com.sabi.logistics.core.models.TollPrices;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.TollPricesRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TollPriceService {

    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;

    @Autowired
    private TollPricesRepository tollPricesRepository;

    public TollPriceService(ModelMapper mapper, ObjectMapper objectMapper, Validations validations) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
    }

    /** <summary>
     * Toll prices creation
     * </summary>
     * <remarks>this method is responsible for creation of new toll price</remarks>
     */

    public TollPricesResponseDto createTollPrice(TollPricesDto request) {
        validations.validateTollPrices(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        log.info("User::::::::::::::::::::::::::::::::::; " + userCurrent);
        TollPrices tollPrices = mapper.map(request,TollPrices.class);
        TollPrices tollPriceExist = tollPricesRepository.findByAssestTypeId(request.getAssestTypeId());
        if(tollPriceExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Toll price already exist");
        }
        tollPrices.setCreatedBy(userCurrent.getId());
        tollPrices.setIsActive(true);
        tollPrices = tollPricesRepository.save(tollPrices);
        log.debug("Create new Toll price - {}"+ new Gson().toJson(tollPrices));
        return mapper.map(tollPrices, TollPricesResponseDto.class);
    }

    public List<TollPricesResponseDto> createTollPrices(List<TollPricesDto> requests) {
        List<TollPricesResponseDto> responseDtos = new ArrayList<>();
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        requests.forEach(request->{
            validations.validateTollPrices(request);
            TollPrices shipmentItem = mapper.map(request,TollPrices.class);
            TollPrices exist = tollPricesRepository.findByAssestTypeId(request.getAssestTypeId());
            if(exist !=null){
                throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " shipment item already exist");
            }
            shipmentItem.setCreatedBy(userCurrent.getId());
            shipmentItem.setIsActive(true);
            shipmentItem = tollPricesRepository.save(shipmentItem);
            log.debug("Create new asset picture - {}"+ new Gson().toJson(shipmentItem));
            responseDtos.add(mapper.map(shipmentItem, TollPricesResponseDto.class));
        });
        return responseDtos;
    }

    /** <summary>
     * Toll price update
     * </summary>
     * <remarks>this method is responsible for updating already existing toll price</remarks>
     */

    public TollPricesResponseDto updateTollPrice(TollPricesDto request) {
        validations.validateTollPrices(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        TollPrices tollPrices = tollPricesRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Toll price Id does not exist!"));
        mapper.map(request, tollPrices);
        tollPrices.setUpdatedBy(userCurrent.getId());
        tollPricesRepository.save(tollPrices);
        log.debug("Toll price record updated - {}"+ new Gson().toJson(tollPrices));
        return mapper.map(tollPrices, TollPricesResponseDto.class);
    }


    /** <summary>
     * Find Toll price
     * </summary>
     * <remarks>this method is responsible for getting a single record</remarks>
     */
    public TollPricesResponseDto findTollPriceById(Long id){
        TollPrices tollPrices = tollPricesRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Toll price id does not exist!"));
        return mapper.map(tollPrices,TollPricesResponseDto.class);
    }


    /** <summary>
     * Find all Toll price
     * </summary>
     * <remarks>this method is responsible for getting all records in pagination</remarks>
     */
    public Page<TollPrices> findAll(Long routeLocationId, Long assestTypeId, PageRequest pageRequest ){
        Page<TollPrices> tollPrices = tollPricesRepository.findTollPrices(routeLocationId,assestTypeId,pageRequest);
        if(tollPrices == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
//        state.getContent().forEach(states -> {
//            Country country = countryRepository.getOne(states.getCountryId());
//
//            states.setCountryName(country.getName());
//        });
        return tollPrices;
    }


    /** <summary>
     * Enable disenable
     * </summary>
     * <remarks>this method is responsible for enabling and dis enabling a Toll price</remarks>
     */
    public void enableDisEnableState (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        TollPrices state = tollPricesRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Toll price Id does not exist!"));
        state.setIsActive(request.isActive());
        state.setUpdatedBy(userCurrent.getId());
        tollPricesRepository.save(state);

    }
}
