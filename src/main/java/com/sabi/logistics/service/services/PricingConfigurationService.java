package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.PricingConfigurationRequest;
import com.sabi.logistics.core.dto.response.PricingConfigurationResponse;
import com.sabi.logistics.core.models.PricingConfiguration;
import com.sabi.logistics.service.helper.GenericSpecification;
import com.sabi.logistics.service.helper.SearchCriteria;
import com.sabi.logistics.service.helper.SearchOperation;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.PricingConfigurationRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class PricingConfigurationService {
    private final PricingConfigurationRepository pricingConfigurationRepository;
    private final ModelMapper mapper;
    @Autowired
    private Validations validations;


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

    /** <summary>
     * pricingConfiguration update
     * </summary>
     * <remarks>this method is responsible for updating already existing pricingConfigurations</remarks>
     */

    public PricingConfigurationResponse updatepricingConfiguration(PricingConfigurationRequest request) {
        validations.validatePricingConfiguration(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PricingConfiguration pricingConfiguration = pricingConfigurationRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested pricingConfiguration Id does not exist!"));
        mapper.map(request, pricingConfiguration);
        pricingConfiguration.setUpdatedBy(userCurrent.getId());
        pricingConfigurationRepository.save(pricingConfiguration);
        log.debug("pricingConfiguration record updated - {}"+ new Gson().toJson(pricingConfiguration));
        return mapper.map(pricingConfiguration, PricingConfigurationResponse.class);
    }


    /** <summary>
     * Find pricingConfiguration
     * </summary>
     * <remarks>this method is responsible for getting a single record</remarks>
     */
    public PricingConfigurationResponse findpricingConfiguration(Long id){
        PricingConfiguration pricingConfiguration = pricingConfigurationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested pricingConfiguration Id does not exist!"));
        return mapper.map(pricingConfiguration, PricingConfigurationResponse.class);
    }


    /** <summary>
     * Find all pricingConfiguration
     * </summary>
     * <remarks>this method is responsible for getting all records in pagination</remarks>
     */
    public Page<PricingConfiguration> findAll(Long partnerId, String routeType, Long stateId,
                                              String locationPreference, BigDecimal pricePerParameter,
                                              BigDecimal pricePerWeight, BigDecimal pricePerDistance, BigDecimal pricePerTime,
                                              Boolean hasPreferentialPricing, PageRequest pageRequest){
        GenericSpecification<PricingConfiguration> genericSpecification = new GenericSpecification<>();
        if (partnerId != null) {
            genericSpecification.add(new SearchCriteria("partnerId", pricePerParameter, SearchOperation.EQUAL));
        }

        if (routeType != null && !routeType.isEmpty()) {
            genericSpecification.add(new SearchCriteria("routeType", routeType, SearchOperation.MATCH));
        }
        if (stateId != null) {
            genericSpecification.add(new SearchCriteria("stateId", stateId, SearchOperation.EQUAL));
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
            genericSpecification.add(new SearchCriteria("pricePerDistance", pricePerTime, SearchOperation.EQUAL));
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
    public void enableDisEnablePricingConfiguration (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PricingConfiguration pricingConfiguration = pricingConfigurationRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested pricingConfiguration Id does not exist!"));
        pricingConfiguration.setIsActive(request.isActive());
        pricingConfiguration.setUpdatedBy(userCurrent.getId());
        pricingConfigurationRepository.save(pricingConfiguration);

    }


    public List<PricingConfiguration> getAll(Boolean isActive){
        List<PricingConfiguration> pricingConfigurations = pricingConfigurationRepository.findByIsActive(isActive);
        return pricingConfigurations;

    }
}
