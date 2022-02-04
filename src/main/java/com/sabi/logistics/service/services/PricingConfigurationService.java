package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.PricingConfigMasterRequest;
import com.sabi.logistics.core.dto.request.PricingConfigurationRequest;
import com.sabi.logistics.core.dto.response.PricingConfigurationResponse;
import com.sabi.logistics.core.dto.response.PricingItemsResponse;
import com.sabi.logistics.core.models.PricingConfiguration;
import com.sabi.logistics.core.models.State;
import com.sabi.logistics.service.helper.GenericSpecification;
import com.sabi.logistics.service.helper.SearchCriteria;
import com.sabi.logistics.service.helper.SearchOperation;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.PricingConfigurationRepository;
import com.sabi.logistics.service.repositories.StateRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("All")
@Slf4j
@Service
public class PricingConfigurationService {
    private final PricingConfigurationRepository pricingConfigurationRepository;
    private final ModelMapper mapper;
    @Autowired
    private Validations validations;

    @Autowired
    private PricingItemsService pricingItemsService;

    @Autowired
    private StateRepository stateRepository;


    public PricingConfigurationService(PricingConfigurationRepository PricingConfigurationRepository, ModelMapper mapper) {
        this.pricingConfigurationRepository = PricingConfigurationRepository;
        this.mapper = mapper;
    }
    public PricingConfigurationResponse createPricingConfiguration(PricingConfigurationRequest request) {
        validations.validatePricingConfiguration(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PricingConfiguration pricingConfiguration = mapper.map(request,PricingConfiguration.class);
//todo:
//        PricingConfiguration pricingConfigurationExists = pricingConfigurationRepository.

//        if(pricingConfigurationExists != null){
//            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "pricingConfiguration already exist");
//        }
        pricingConfiguration.setCreatedBy(userCurrent.getId());
        pricingConfiguration.setIsActive(true);
        pricingConfiguration = pricingConfigurationRepository.save(pricingConfiguration);
        log.debug("Create new tripRequestResponse - {}"+ new Gson().toJson(pricingConfiguration));
        return  mapper.map(pricingConfiguration, PricingConfigurationResponse.class);
    }

    public PricingConfigurationResponse createMasterPricingConfiguration(PricingConfigMasterRequest request) {
        validations.validatePricingConfiguration(request);
        List<PricingItemsResponse> pricingItemsResponses = new ArrayList<>();
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PricingConfiguration pricingConfiguration = mapper.map(request,PricingConfiguration.class);

        if (request.getDestinationLocations() != null) {
            Set<String> set = new HashSet<>(Arrays.asList(request.getDestinationLocations().toArray(new String[0])));
            String destinationLocations = String.join(", ", set);
            pricingConfiguration.setDestinationLocations(destinationLocations);
        }
        pricingConfiguration.setCreatedBy(userCurrent.getId());
        pricingConfiguration.setIsActive(true);
        pricingConfiguration = pricingConfigurationRepository.save(pricingConfiguration);
        log.debug("Create new tripRequestResponse - {}"+ new Gson().toJson(pricingConfiguration));
        PricingConfigurationResponse pricingConfigurationResponse =  mapper.map(pricingConfiguration, PricingConfigurationResponse.class);

        if (pricingConfigurationResponse.getArrivalStateId() != null) {
            State state = stateRepository.findStateById(pricingConfigurationResponse.getArrivalStateId());
            pricingConfigurationResponse.setStateName(state.getName());
        }
        if (pricingConfigurationResponse.getDepartureStateId() != null) {
            State departureState = stateRepository.findStateById(pricingConfigurationResponse.getDepartureStateId());
            pricingConfigurationResponse.setDepartureStateName(departureState.getName());
        }

        if(pricingConfiguration.getDestinationLocations() != null) {
            String str = pricingConfiguration.getDestinationLocations();
            Set<String> set = Stream.of(str.trim().split("\\s*,\\s*")).collect(Collectors.toSet());
            pricingConfigurationResponse.setDestinationLocations(set);
        }

        if(request.getPricingItems() != null) {
            pricingItemsResponses = pricingItemsService.createPricingItems(request.getPricingItems(), pricingConfigurationResponse.getId());
            List<PricingItemsResponse> finalPricingItemsResponse = pricingItemsResponses;
            pricingItemsResponses.forEach(response -> {
                pricingConfigurationResponse.setPricingItems(finalPricingItemsResponse);
            });
        }

        return pricingConfigurationResponse;
    }

    /** <summary>
     * pricingConfiguration update
     * </summary>
     * <remarks>this method is responsible for updating already existing pricingConfigurations</remarks>
     */

    public PricingConfigurationResponse updatePricingConfiguration(PricingConfigurationRequest request) {
        validations.validatePricingConfiguration(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PricingConfiguration pricingConfiguration = pricingConfigurationRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested pricingConfiguration Id does not exist!"));
        mapper.map(request, pricingConfiguration);

        if (request.getDestinationLocations() != null){
            Set<String> set = new HashSet<>(Arrays.asList(request.getDestinationLocations().toArray(new String[0])));
            String destinationLocations = String.join(", ", set);
            pricingConfiguration.setDestinationLocations(destinationLocations);
        }


        pricingConfiguration.setUpdatedBy(userCurrent.getId());
        pricingConfigurationRepository.save(pricingConfiguration);
        log.debug("pricingConfiguration record updated - {}"+ new Gson().toJson(pricingConfiguration));
        PricingConfigurationResponse pricingConfigurationResponse = mapper.map(pricingConfiguration, PricingConfigurationResponse.class);

        if (pricingConfigurationResponse.getArrivalStateId() != null) {
            State state = stateRepository.findStateById(pricingConfigurationResponse.getArrivalStateId());
            pricingConfigurationResponse.setStateName(state.getName());
        }
        if (pricingConfigurationResponse.getDepartureStateId() != null) {
            State departureState = stateRepository.findStateById(pricingConfigurationResponse.getDepartureStateId());
            pricingConfigurationResponse.setDepartureStateName(departureState.getName());
        }

        if(pricingConfiguration.getDestinationLocations() != null) {
            String str = pricingConfiguration.getDestinationLocations();
            Set<String> set = Stream.of(str.trim().split("\\s*,\\s*")).collect(Collectors.toSet());
            pricingConfigurationResponse.setDestinationLocations(set);
        }

        return pricingConfigurationResponse;
    }


    /** <summary>
     * Find pricingConfiguration
     * </summary>
     * <remarks>this method is responsible for getting a single record</remarks>
     */
    public PricingConfigurationResponse findPricingConfiguration(Long id){
        PricingConfiguration pricingConfiguration = pricingConfigurationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested pricingConfiguration Id does not exist!"));
        PricingConfigurationResponse pricingConfigurationResponse = mapper.map(pricingConfiguration, PricingConfigurationResponse.class);

        State state = stateRepository.findStateById(pricingConfiguration.getArrivalStateId());
        State departureState = stateRepository.findStateById(pricingConfiguration.getDepartureStateId());
        pricingConfigurationResponse.setStateName(state.getName());
        pricingConfigurationResponse.setDepartureStateName(departureState.getName());

        return pricingConfigurationResponse;
    }


    /** <summary>
     * Find all pricingConfiguration
     * </summary>
     * <remarks>this method is responsible for getting all records in pagination</remarks>
     */
    public Page<PricingConfiguration> findAll(Long partnerId, String routeType, Long arrivalStateId,
                                              String locationPreference, String startingLocation, BigDecimal pricePerParameter,
                                              BigDecimal pricePerWeight, BigDecimal pricePerDistance, BigDecimal pricePerTime,
                                              Boolean hasPreferentialPricing, PageRequest pageRequest){
        GenericSpecification<PricingConfiguration> genericSpecification = new GenericSpecification<>();
        if (partnerId != null) {
            genericSpecification.add(new SearchCriteria("partnerId", pricePerParameter, SearchOperation.EQUAL));
        }

        if (routeType != null && !routeType.isEmpty()) {
            genericSpecification.add(new SearchCriteria("routeType", routeType, SearchOperation.MATCH));
        }
        if (arrivalStateId != null) {
            genericSpecification.add(new SearchCriteria("arrivalStateId", arrivalStateId, SearchOperation.EQUAL));
        }
        if (locationPreference != null && !locationPreference.isEmpty()) {
            genericSpecification.add(new SearchCriteria("locationPreference", locationPreference, SearchOperation.MATCH));
        }
        if (pricePerParameter != null) {
            genericSpecification.add(new SearchCriteria("pricePerParameter", pricePerParameter, SearchOperation.EQUAL));
        }
        if (pricePerWeight != null) {
            genericSpecification.add(new SearchCriteria("pricePerWeight", pricePerWeight, SearchOperation.EQUAL));
        }
        if (pricePerDistance != null) {
            genericSpecification.add(new SearchCriteria("pricePerDistance", pricePerDistance, SearchOperation.EQUAL));
        }
        if (pricePerTime != null) {
            genericSpecification.add(new SearchCriteria("pricePerTime", pricePerTime, SearchOperation.EQUAL));
        }
        if (startingLocation != null) {
            genericSpecification.add(new SearchCriteria("startingLocation", startingLocation, SearchOperation.MATCH));
        }
        if (hasPreferentialPricing != null) {
            genericSpecification.add(new SearchCriteria("hasPreferentialPricing", hasPreferentialPricing, SearchOperation.EQUAL));
        }
        Page<PricingConfiguration> pricingConfigurations = pricingConfigurationRepository.findAll(genericSpecification, pageRequest);
        return pricingConfigurations;
    }


    /** <summary>
     * Enable disenable
     * </summary>
     * <remarks>this method is responsible for enabling and dis enabling a pricingConfiguration</remarks>
     */
    public void enableDisablePricingConfiguration (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PricingConfiguration pricingConfiguration = pricingConfigurationRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested pricingConfiguration Id does not exist!"));
        pricingConfiguration.setIsActive(request.isActive());
        pricingConfiguration.setUpdatedBy(userCurrent.getId());
        pricingConfigurationRepository.save(pricingConfiguration);

    }


    public List<PricingConfigurationResponse> getAll(Boolean isActive){
        List<PricingConfigurationResponse> responseDtos = new ArrayList<>();

        List<PricingConfiguration> pricingConfigurations = pricingConfigurationRepository.findByIsActive(isActive);

        pricingConfigurations.forEach(config -> {
            PricingConfigurationResponse pricingConfigurationResponse = mapper.map(config, PricingConfigurationResponse.class);

            if (pricingConfigurationResponse.getArrivalStateId() != null) {
                State state = stateRepository.findStateById(pricingConfigurationResponse.getArrivalStateId());
                pricingConfigurationResponse.setStateName(state.getName());
            }
            if (pricingConfigurationResponse.getDepartureStateId() != null) {
                State departureState = stateRepository.findStateById(pricingConfigurationResponse.getDepartureStateId());
                pricingConfigurationResponse.setDepartureStateName(departureState.getName());
            }

            if(config.getDestinationLocations() != null) {
                String str = config.getDestinationLocations();
                Set<String> set = Stream.of(str.trim().split("\\s*,\\s*")).collect(Collectors.toSet());
                pricingConfigurationResponse.setDestinationLocations(set);
            }
            responseDtos.add(pricingConfigurationResponse);
        });
        return responseDtos;

    }
}
