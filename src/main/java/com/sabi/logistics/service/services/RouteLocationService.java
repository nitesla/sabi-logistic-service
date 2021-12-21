package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.RouteLocationRequest;
import com.sabi.logistics.core.dto.response.RouteLocationResponse;
import com.sabi.logistics.core.models.RouteLocation;
import com.sabi.logistics.service.helper.GenericSpecification;
import com.sabi.logistics.service.helper.SearchCriteria;
import com.sabi.logistics.service.helper.SearchOperation;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.RouteLocationRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class RouteLocationService {
    private final RouteLocationRepository routeLocationRepository;
    private final ModelMapper mapper;
    @Autowired
    private Validations validations;


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
        routeLocation.setCreatedBy(userCurrent.getId());
        routeLocation.setIsActive(true);
        routeLocation = routeLocationRepository.save(routeLocation);
        log.debug("Create new tripRequestResponse - {}" + new Gson().toJson(routeLocation));
        return mapper.map(routeLocation, RouteLocationResponse.class);
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
