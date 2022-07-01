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
import com.sabi.logistics.core.enums.DynamicType;
import com.sabi.logistics.core.models.*;
import com.sabi.logistics.service.helper.GenericSpecification;
import com.sabi.logistics.service.helper.SearchCriteria;
import com.sabi.logistics.service.helper.SearchOperation;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.*;
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

    @Autowired
    private PricingItemsRepository pricingItemsRepository;

    @Autowired
    private PartnerAssetTypeRepository partnerAssetTypeRepository;

    @Autowired
    private AssetTypePropertiesRepository assetTypePropertiesRepository;

    @Autowired
    private RouteLocationRepository routeLocationRepository;


    public PricingConfigurationService(PricingConfigurationRepository PricingConfigurationRepository, ModelMapper mapper) {
        this.pricingConfigurationRepository = PricingConfigurationRepository;
        this.mapper = mapper;
    }
    public PricingConfigurationResponse createPricingConfiguration(PricingConfigurationRequest request) {
//        validations.validatePricingConfiguration(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PricingConfiguration pricingConfiguration = mapper.map(request,PricingConfiguration.class);
        pricingConfiguration.setCreatedBy(userCurrent.getId());
        pricingConfiguration.setIsActive(true);
        pricingConfiguration = pricingConfigurationRepository.save(pricingConfiguration);
        log.debug("Create new tripRequestResponse - {}"+ new Gson().toJson(pricingConfiguration));
        return  mapper.map(pricingConfiguration, PricingConfigurationResponse.class);
    }

    public PricingConfigurationResponse createMasterPricingConfiguration(PricingConfigMasterRequest request) {
//        validations.validatePricingConfiguration(request);
        List<PricingItemsResponse> pricingItemsResponses = new ArrayList<>();
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PricingConfiguration pricingConfiguration = mapper.map(request,PricingConfiguration.class);

        if (request.getDestinationLocations() != null) {
            Set<String> set = new HashSet<>(Arrays.asList(request.getDestinationLocations().toArray(new String[0])));
            String destinationLocations = String.join(", ", set);
            pricingConfiguration.setDestinationLocations(destinationLocations);
        }

        if (request.getStartingLocations() != null) {
            Set<String> set = new HashSet<>(Arrays.asList(request.getStartingLocations().toArray(new String[0])));
            String startingLocations = String.join(", ", set);
            pricingConfiguration.setStartingLocations(startingLocations);
        }
        pricingConfiguration.setCreatedBy(userCurrent.getId());
        pricingConfiguration.setIsActive(true);
        pricingConfiguration = pricingConfigurationRepository.save(pricingConfiguration);
        log.debug("Create new tripRequestResponse - {}"+ new Gson().toJson(pricingConfiguration));
        PricingConfigurationResponse pricingConfigurationResponse =  mapper.map(pricingConfiguration, PricingConfigurationResponse.class);

        if (pricingConfigurationResponse.getArrivalStateId() != null) {
//            State arrivalState = stateRepository.findStateById(pricingConfigurationResponse.getArrivalStateId());
            pricingConfigurationResponse.setArrivalStateName(request.getArrivalStateName());
        }
        if (pricingConfigurationResponse.getDepartureStateId() != null) {
//            State departureState = stateRepository.findStateById(pricingConfigurationResponse.getDepartureStateId());
            pricingConfigurationResponse.setDepartureStateName(request.getDepartureStateName());
        }

        if(pricingConfiguration.getDestinationLocations() != null) {
            String str = pricingConfiguration.getDestinationLocations();
            Set<String> set = Stream.of(str.trim().split("\\s*,\\s*")).collect(Collectors.toSet());
            pricingConfigurationResponse.setDestinationLocations(set);
        }

        if(pricingConfiguration.getStartingLocations() != null) {
            String str = pricingConfiguration.getStartingLocations();
            Set<String> set = Stream.of(str.trim().split("\\s*,\\s*")).collect(Collectors.toSet());
            pricingConfigurationResponse.setStartingLocations(set);
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
//        validations.validatePricingConfiguration(request);
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

        if (request.getStartingLocations() != null){
            Set<String> set = new HashSet<>(Arrays.asList(request.getStartingLocations().toArray(new String[0])));
            String startingLocations = String.join(", ", set);
            pricingConfiguration.setStartingLocations(startingLocations);
        }


        pricingConfiguration.setUpdatedBy(userCurrent.getId());
        pricingConfigurationRepository.save(pricingConfiguration);
        log.debug("pricingConfiguration record updated - {}"+ new Gson().toJson(pricingConfiguration));
        PricingConfigurationResponse pricingConfigurationResponse = mapper.map(pricingConfiguration, PricingConfigurationResponse.class);

        if (pricingConfigurationResponse.getArrivalStateId() != null) {
//            State arrivalState = stateRepository.findStateById(pricingConfigurationResponse.getArrivalStateId());
            pricingConfigurationResponse.setArrivalStateName(request.getArrivalStateName());
        }
        if (pricingConfigurationResponse.getDepartureStateId() != null) {
//            State departureState = stateRepository.findStateById(pricingConfigurationResponse.getDepartureStateId());
            pricingConfigurationResponse.setDepartureStateName(request.getDepartureStateName());
        }

        if(pricingConfiguration.getDestinationLocations() != null) {
            String str = pricingConfiguration.getDestinationLocations();
            Set<String> set = Stream.of(str.trim().split("\\s*,\\s*")).collect(Collectors.toSet());
            pricingConfigurationResponse.setDestinationLocations(set);
        }

        if(pricingConfiguration.getStartingLocations() != null) {
            String str = pricingConfiguration.getStartingLocations();
            Set<String> set = Stream.of(str.trim().split("\\s*,\\s*")).collect(Collectors.toSet());
            pricingConfigurationResponse.setStartingLocations(set);
        }

        pricingConfigurationResponse.setPricingItems(getAllPricingItems(pricingConfigurationResponse.getId()));

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

//        State arrivalState = stateRepository.findStateById(pricingConfiguration.getArrivalStateId());
//        State departureState = stateRepository.findStateById(pricingConfiguration.getDepartureStateId());
        pricingConfigurationResponse.setArrivalStateName(pricingConfiguration.getArrivalStateName());
        pricingConfigurationResponse.setDepartureStateName(pricingConfiguration.getDepartureStateName());

        if(pricingConfiguration.getDestinationLocations() != null) {
            String str = pricingConfiguration.getDestinationLocations();
            Set<String> sets = Stream.of(str.trim().split("\\s*,\\s*")).collect(Collectors.toSet());
            pricingConfigurationResponse.setDestinationLocations(sets);
            Set<String> destinationLocationNames = new HashSet<>();
            sets.forEach(set -> {
                RouteLocation location = routeLocationRepository.findRouteLocationById(Long.parseLong(set.replace("[", "").replace("]","")));
                String name = location.getName();
                destinationLocationNames.add(name);
            });
            pricingConfigurationResponse.setDestinationLocationNames(destinationLocationNames);
        }

        if(pricingConfiguration.getStartingLocations() != null) {
            String str = pricingConfiguration.getStartingLocations();
            Set<String> sets = Stream.of(str.trim().split("\\s*,\\s*")).collect(Collectors.toSet());
            pricingConfigurationResponse.setStartingLocations(sets);
            Set<String> startingLocationNames = new HashSet<>();
            sets.forEach(set -> {

                RouteLocation location = routeLocationRepository.findRouteLocationById(Long.parseLong(set.replace("[", "").replace("]","")));
                String name = location.getName();
                startingLocationNames.add(name);
            });
            pricingConfigurationResponse.setStartingLocationNames(startingLocationNames);
        }

        pricingConfigurationResponse.setPricingItems(getAllPricingItems(pricingConfigurationResponse.getId()));

        return pricingConfigurationResponse;
    }


    /** <summary>
     * Find all pricingConfiguration
     * </summary>
     * <remarks>this method is responsible for getting all records in pagination</remarks>
     */
    public Page<PricingConfiguration> findAll(Long partnerId, String routeType, Long arrivalStateId,
                                              String locationPreference,  BigDecimal pricePerParameter,
                                              BigDecimal pricePerWeight, BigDecimal pricePerDistance, BigDecimal pricePerTime,
                                              Boolean hasPreferentialPricing, DynamicType dynamicType, String tripType, PageRequest pageRequest){
        GenericSpecification<PricingConfiguration> genericSpecification = new GenericSpecification<>();
        if (partnerId != null) {
            genericSpecification.add(new SearchCriteria("partnerId", partnerId, SearchOperation.EQUAL));
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
        if (hasPreferentialPricing != null) {
            genericSpecification.add(new SearchCriteria("hasPreferentialPricing", hasPreferentialPricing, SearchOperation.EQUAL));
        }
        if (dynamicType != null) {
            genericSpecification.add(new SearchCriteria("dynamicType", dynamicType, SearchOperation.EQUAL));
        }
        if (tripType != null && !tripType.isEmpty()) {
            genericSpecification.add(new SearchCriteria("tripType", tripType, SearchOperation.MATCH));
        }
        Page<PricingConfiguration> pricingConfigurations = pricingConfigurationRepository.findAll(genericSpecification, pageRequest);
        if(pricingConfigurations == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        pricingConfigurations.getContent().forEach(config -> {

//            if (config.getArrivalStateId() != null) {
//                State arrivalState = stateRepository.findStateById(config.getArrivalStateId());
//                config.setArrivalStateName(arrivalState.getName());
//            }
//            if (config.getDepartureStateId() != null) {
//                State departureState = stateRepository.findStateById(config.getDepartureStateId());
//                config.setDepartureStateName(departureState.getName());
//            }

            if(config.getDestinationLocations() != null) {
                String str = config.getDestinationLocations();
                Set<String> sets = Stream.of(str.trim().split("\\s*,\\s*")).collect(Collectors.toSet());
                Set<String> destinationLocationNames = new HashSet<>();
                sets.forEach(set -> {
                        RouteLocation location = routeLocationRepository.findRouteLocationById(Long.parseLong(set.replace("[", "").replace("]","")));
                        String name = location.getName();
                        destinationLocationNames.add(name);
                });
                config.setDestinationLocationNames(destinationLocationNames);
            }

            if(config.getStartingLocations() != null) {
                String str = config.getStartingLocations();
                Set<String> sets = Stream.of(str.trim().split("\\s*,\\s*")).collect(Collectors.toSet());
                Set<String> startingLocationNames = new HashSet<>();
                sets.forEach(set -> {

                    RouteLocation location = routeLocationRepository.findRouteLocationById(Long.parseLong(set.replace("[", "").replace("]","")));
                    String name = location.getName();
                    startingLocationNames.add(name);
                });
                config.setStartingLocationNames(startingLocationNames);
            }

        });
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
        pricingConfiguration.setIsActive(request.getIsActive());
        pricingConfiguration.setUpdatedBy(userCurrent.getId());
        pricingConfigurationRepository.save(pricingConfiguration);

    }


    public List<PricingConfigurationResponse> getAll(Long partnerId, Boolean isActive){
        List<PricingConfigurationResponse> responseDtos = new ArrayList<>();

        List<PricingConfiguration> pricingConfigurations = pricingConfigurationRepository.findByPartnerIdAndIsActive(partnerId, isActive);

        pricingConfigurations.forEach(config -> {
            PricingConfigurationResponse pricingConfigurationResponse = mapper.map(config, PricingConfigurationResponse.class);

//            if (pricingConfigurationResponse.getArrivalStateId() != null) {
//                State arrivalState = stateRepository.findStateById(pricingConfigurationResponse.getArrivalStateId());
//                pricingConfigurationResponse.setArrivalStateName(arrivalState.getName());
//            }
//            if (pricingConfigurationResponse.getDepartureStateId() != null) {
//                State departureState = stateRepository.findStateById(pricingConfigurationResponse.getDepartureStateId());
//                pricingConfigurationResponse.setDepartureStateName(departureState.getName());
//            }

            if(config.getDestinationLocations() != null) {
                String str = config.getDestinationLocations();
                Set<String> sets = Stream.of(str.trim().split("\\s*,\\s*")).collect(Collectors.toSet());
                pricingConfigurationResponse.setDestinationLocations(sets);

                Set<String> destinationLocationNames = new HashSet<>();
                sets.forEach(set -> {
                    RouteLocation location = routeLocationRepository.findRouteLocationById(Long.parseLong(set.replace("[", "").replace("]","")));
                    String name = location.getName();
                    destinationLocationNames.add(name);
                });
                pricingConfigurationResponse.setDestinationLocationNames(destinationLocationNames);
            }

            if(config.getStartingLocations() != null) {
                String str = config.getStartingLocations();
                Set<String> sets = Stream.of(str.trim().split("\\s*,\\s*")).collect(Collectors.toSet());
                pricingConfigurationResponse.setStartingLocations(sets);

                Set<String> startingLocationNames = new HashSet<>();
                sets.forEach(set -> {

                    RouteLocation location = routeLocationRepository.findRouteLocationById(Long.parseLong(set.replace("[", "").replace("]","")));
                    String name = location.getName();
                    startingLocationNames.add(name);
                });
                pricingConfigurationResponse.setStartingLocationNames(startingLocationNames);
            }

            pricingConfigurationResponse.setPricingItems(getAllPricingItems(config.getId()));

            responseDtos.add(pricingConfigurationResponse);
        });
        return responseDtos;

    }

    public List<PricingItemsResponse> getAllPricingItems(Long pricingConfigurationId){
        List<PricingItemsResponse> responseDtos = new ArrayList<>();
        List<PricingItems> pricingItems = pricingItemsRepository.findByPricingConfigurationId(pricingConfigurationId);

        for (PricingItems pricingItem : pricingItems) {
            PricingItemsResponse PricingItemsResponse = mapper.map(pricingItem, PricingItemsResponse.class);

            PartnerAssetType partnerAssetType = partnerAssetTypeRepository.getOne(pricingItem.getPartnerAssetTypeId());
            AssetTypeProperties assetTypeProperties = assetTypePropertiesRepository.findAssetTypePropertiesById(partnerAssetType.getAssetTypeId());
            PricingItemsResponse.setAssetTypeId(assetTypeProperties.getId());
            PricingItemsResponse.setAssetTypeName(assetTypeProperties.getName());
            responseDtos.add(PricingItemsResponse);
        }

        return responseDtos;

    }
}
