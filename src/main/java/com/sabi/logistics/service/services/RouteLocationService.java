package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.RouteLocationRequest;
import com.sabi.logistics.core.dto.request.RouteLocationTollPriceRequest;
import com.sabi.logistics.core.dto.response.RouteLocationResponse;
import com.sabi.logistics.core.dto.response.RouteLocationTollPriceResponse;
import com.sabi.logistics.core.dto.response.TollPricesResponseDto;
import com.sabi.logistics.core.models.RouteLocation;
import com.sabi.logistics.core.models.State;
import com.sabi.logistics.core.models.TollPrices;
import com.sabi.logistics.service.helper.GenericSpecification;
import com.sabi.logistics.service.helper.SearchCriteria;
import com.sabi.logistics.service.helper.SearchOperation;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.RouteLocationRepository;
import com.sabi.logistics.service.repositories.StateRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class RouteLocationService {
    private final RouteLocationRepository routeLocationRepository;
    private final ModelMapper mapper;
    @Autowired
    private Validations validations;
    @Autowired
    private StateRepository stateRepository;
    @Autowired
    private TollPriceService tollPriceService;


    public RouteLocationService(RouteLocationRepository routeLocationRepository, ModelMapper mapper) {
        this.routeLocationRepository = routeLocationRepository;
        this.mapper = mapper;
    }

    public RouteLocationResponse createrouteLocation(RouteLocationRequest request) {
        validations.validaterouteLocation(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        RouteLocation routeLocation = mapper.map(request, RouteLocation.class);

        RouteLocation routeLocationExists = routeLocationRepository.findByName(request.getName());

        if (routeLocationExists != null) {
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "routeLocation already exist");
        }
        State SavedState = stateRepository.findStateById(request.getStateId());
        routeLocation.setCreatedBy(userCurrent.getId());
        routeLocation.setIsActive(true);
        routeLocation.setStateName(SavedState.getName());
        routeLocation = routeLocationRepository.save(routeLocation);
        log.debug("Create new tripRequestResponse - {}" + new Gson().toJson(routeLocation));
        return mapper.map(routeLocation, RouteLocationResponse.class);
    }

    public RouteLocationTollPriceResponse createrouteLocationTollPrice(RouteLocationTollPriceRequest request) {
        List<TollPricesResponseDto> responseDtos = new ArrayList<>();
        validations.validaterouteLocationTollPrice(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        RouteLocation routeLocation = mapper.map(request,RouteLocation.class);
        TollPrices shipmentItem = mapper.map(request, TollPrices.class);

        RouteLocation routeLocationExists = routeLocationRepository.findByName(request.getName());
        if (routeLocationExists != null) {
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "routeLocation already exist");
        }
        State SavedState = stateRepository.findStateById(request.getStateId());
        routeLocation.setCreatedBy(userCurrent.getId());
        routeLocation.setIsActive(true);
        routeLocation.setStateName(SavedState.getName());
        log.debug("Create new shipment - {}"+ new Gson().toJson(routeLocation));
        RouteLocationTollPriceResponse orderResponseDto = mapper.map(routeLocation, RouteLocationTollPriceResponse.class);
        log.info("request sent ::::::::::::::::::::::::::::::::: " + request.getTollPricesDtos());
        request.getTollPricesDtos().forEach(orderItemRequest ->{
            orderItemRequest.setId(orderResponseDto.getId());
        });
        responseDtos = tollPriceService.createTollPrices(request.getTollPricesDtos());
        List<TollPricesResponseDto> finalResponseDtos = responseDtos;
        responseDtos.forEach(orderItemResponseDto -> {
            orderResponseDto.setTollPricesResponseDtos(finalResponseDtos);
        });
        return orderResponseDto;
    }

    /**
     * <summary>
     * routeLocation update
     * </summary>
     * <remarks>this method is responsible for updating already existing routeLocations</remarks>
     */

    public RouteLocationResponse updaterouteLocation(RouteLocationRequest request) {
        validations.validaterouteLocation(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        RouteLocation routeLocation = routeLocationRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested routeLocation Id does not exist!"));
        mapper.map(request, routeLocation);
        routeLocation.setUpdatedBy(userCurrent.getId());
        routeLocationRepository.save(routeLocation);
        log.debug("routeLocation record updated - {}" + new Gson().toJson(routeLocation));
        return mapper.map(routeLocation, RouteLocationResponse.class);
    }


    /**
     * <summary>
     * Find routeLocation
     * </summary>
     * <remarks>this method is responsible for getting a single record</remarks>
     */
    public RouteLocationResponse findrouteLocation(Long id) {
        RouteLocation routeLocation = routeLocationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested routeLocation Id does not exist!"));
        return mapper.map(routeLocation, RouteLocationResponse.class);
    }

//    public RouteLocationResponse findrouteLocationByStateId(Long StateId) {
//        RouteLocation routeLocation = routeLocationRepository.findRouteLocationByStateId(StateId);
//        if (routeLocation == null){
//                throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
//                        "Requested routeLocation Id does not exist!");
//        }
//
//        return mapper.map(routeLocation, RouteLocationResponse.class);
//    }

    public List<RouteLocation> findrouteLocationByStateId(Long StateId) {
        List<RouteLocation> routeLocations = routeLocationRepository.findByStateId(StateId);
        return routeLocations;

    }


    /**
     * <summary>
     * Find all routeLocation
     * </summary>
     * <remarks>this method is responsible for getting all records in pagination</remarks>
     */
    public Page<RouteLocation> findAll(String name, Long stateId,
                                      BigDecimal tollRate, Boolean hasToll,PageRequest pageRequest) {
        GenericSpecification<RouteLocation> genericSpecification = new GenericSpecification<>();
        if (name != null && !name.isEmpty()) {
            genericSpecification.add(new SearchCriteria("name", name, SearchOperation.MATCH));
        }

        if (stateId != null) {
            genericSpecification.add(new SearchCriteria("stateId", stateId, SearchOperation.EQUAL));
        }
        if (tollRate != null) {
            genericSpecification.add(new SearchCriteria("tollRate", tollRate, SearchOperation.EQUAL));
        }
        if (hasToll != null)
            genericSpecification.add(new SearchCriteria("hasToll", hasToll, SearchOperation.EQUAL));

        Page<RouteLocation> routeLocations = routeLocationRepository.findAll(genericSpecification, pageRequest);
        return routeLocations;
    }


    /**
     * <summary>
     * Enable disenable
     * </summary>
     * <remarks>this method is responsible for enabling and dis enabling a routeLocation</remarks>
     */
    public void enableDisEnablerouteLocation(EnableDisEnableDto request) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        RouteLocation routeLocation = routeLocationRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested routeLocation Id does not exist!"));
        routeLocation.setIsActive(request.isActive());
        routeLocation.setUpdatedBy(userCurrent.getId());
        routeLocationRepository.save(routeLocation);

    }


    public List<RouteLocation> getAll(Boolean isActive) {
        List<RouteLocation> routeLocations = routeLocationRepository.findByIsActive(isActive);
        return routeLocations;

    }
}
