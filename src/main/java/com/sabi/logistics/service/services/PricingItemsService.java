package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.PricingItemsRequest;
import com.sabi.logistics.core.dto.response.PricingItemsResponse;
import com.sabi.logistics.core.models.AssetTypeProperties;
import com.sabi.logistics.core.models.PartnerAssetType;
import com.sabi.logistics.core.models.PricingItems;
import com.sabi.logistics.service.helper.GenericSpecification;
import com.sabi.logistics.service.helper.SearchCriteria;
import com.sabi.logistics.service.helper.SearchOperation;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.AssetTypePropertiesRepository;
import com.sabi.logistics.service.repositories.PartnerAssetTypeRepository;
import com.sabi.logistics.service.repositories.PricingItemsRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PricingItemsService {

    private final PricingItemsRepository pricingItemsRepository;
    private final ModelMapper mapper;
    @Autowired
    private Validations validations;

    @Autowired
    private PartnerAssetTypeRepository partnerAssetTypeRepository;

    @Autowired
    private AssetTypePropertiesRepository assetTypePropertiesRepository;


    public PricingItemsService(PricingItemsRepository pricingItemsRepository, ModelMapper mapper) {
        this.pricingItemsRepository = pricingItemsRepository;
        this.mapper = mapper;
    }

    public PricingItemsResponse createPricingItem(PricingItemsRequest request) {
        validations.validatePricingItem(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PricingItems pricingItems = mapper.map(request, PricingItems.class);

        PricingItems pricingItemsExists = pricingItemsRepository.findByPartnerAssetTypeId(request.getPartnerAssetTypeId());

        if (pricingItemsExists != null) {
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "pricingItems already exist");
        }
        PartnerAssetType partnerAssetType = partnerAssetTypeRepository.getOne(request.getPartnerAssetTypeId());
        AssetTypeProperties assetTypeProperties = assetTypePropertiesRepository.findAssetTypePropertiesById(partnerAssetType.getAssetTypeId());

        pricingItems.setCreatedBy(userCurrent.getId());
        pricingItems.setIsActive(true);
        pricingItems = pricingItemsRepository.save(pricingItems);
        log.debug("Create new tripRequestResponse - {}" + new Gson().toJson(pricingItems));
        PricingItemsResponse pricingItemsResponse = mapper.map(pricingItems, PricingItemsResponse.class);

        pricingItemsResponse.setAssetTypeId(assetTypeProperties.getId());
        pricingItemsResponse.setAssetTypeName(assetTypeProperties.getName());

        return pricingItemsResponse;
    }

    public List<PricingItemsResponse> createPricingItems(List<PricingItemsRequest> requests, Long pricingConfigurationId ) {
        List<PricingItemsResponse> responseDtos = new ArrayList<>();
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        requests.forEach(request-> {

            request.setPricingConfigurationId(pricingConfigurationId);
            validations.validatePricingItem(request);
            PricingItems pricingItems = mapper.map(request, PricingItems.class);

            PricingItems pricingItemsExists = pricingItemsRepository.findByPartnerAssetTypeId(request.getPartnerAssetTypeId());

            if (pricingItemsExists != null) {
                throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "pricingItems already exist");
            }
            PartnerAssetType partnerAssetType = partnerAssetTypeRepository.getOne(request.getPartnerAssetTypeId());
            AssetTypeProperties assetTypeProperties = assetTypePropertiesRepository.findAssetTypePropertiesById(partnerAssetType.getAssetTypeId());

            pricingItems.setCreatedBy(userCurrent.getId());
            pricingItems.setIsActive(true);
            pricingItems = pricingItemsRepository.save(pricingItems);
            log.debug("Create new tripRequestResponse - {}" + new Gson().toJson(pricingItems));
            PricingItemsResponse pricingItemsResponse = mapper.map(pricingItems, PricingItemsResponse.class);

            pricingItemsResponse.setAssetTypeId(assetTypeProperties.getId());
            pricingItemsResponse.setAssetTypeName(assetTypeProperties.getName());

            responseDtos.add(pricingItemsResponse);

        });
        return responseDtos;
    }

    /**
     * <summary>
     * pricingItems update
     * </summary>
     * <remarks>this method is responsible for updating already existing pricingItemss</remarks>
     */

    public PricingItemsResponse updatePricingItem(PricingItemsRequest request) {
        validations.validatePricingItem(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PricingItems pricingItems = pricingItemsRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested pricingItems Id does not exist!"));
        mapper.map(request, pricingItems);

        PartnerAssetType partnerAssetType = partnerAssetTypeRepository.getOne(request.getPartnerAssetTypeId());
        AssetTypeProperties assetTypeProperties = assetTypePropertiesRepository.findAssetTypePropertiesById(partnerAssetType.getAssetTypeId());

        pricingItems.setUpdatedBy(userCurrent.getId());
        pricingItemsRepository.save(pricingItems);
        log.debug("pricingItems record updated - {}" + new Gson().toJson(pricingItems));
        PricingItemsResponse pricingItemsResponse = mapper.map(pricingItems, PricingItemsResponse.class);

        pricingItemsResponse.setAssetTypeId(assetTypeProperties.getId());
        pricingItemsResponse.setAssetTypeName(assetTypeProperties.getName());

        return pricingItemsResponse;
    }


    /**
     * <summary>
     * Find pricingItems
     * </summary>
     * <remarks>this method is responsible for getting a single record</remarks>
     */
    public PricingItemsResponse findPricingItem(Long id) {
        PricingItems pricingItems = pricingItemsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested pricingItems Id does not exist!"));
        PricingItemsResponse pricingItemsResponse = mapper.map(pricingItems, PricingItemsResponse.class);
        PartnerAssetType partnerAssetType = partnerAssetTypeRepository.getOne(pricingItems.getPartnerAssetTypeId());
        AssetTypeProperties assetTypeProperties = assetTypePropertiesRepository.findAssetTypePropertiesById(partnerAssetType.getAssetTypeId());
        pricingItemsResponse.setAssetTypeId(assetTypeProperties.getId());
        pricingItemsResponse.setAssetTypeName(assetTypeProperties.getName());

        return pricingItemsResponse;
    }

    public PricingItemsResponse deletePricingItem(Long id){
        PricingItems pricingItems = pricingItemsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested pricingItems Id does not exist!"));
        pricingItemsRepository.deleteById(pricingItems.getId());
        log.debug("pricingItems Deleted - {}"+ new Gson().toJson(pricingItems));
        PricingItemsResponse pricingItemsResponse = mapper.map(pricingItems, PricingItemsResponse.class);
        PartnerAssetType partnerAssetType = partnerAssetTypeRepository.getOne(pricingItems.getPartnerAssetTypeId());
        AssetTypeProperties assetTypeProperties = assetTypePropertiesRepository.findAssetTypePropertiesById(partnerAssetType.getAssetTypeId());
        pricingItemsResponse.setAssetTypeId(assetTypeProperties.getId());
        pricingItemsResponse.setAssetTypeName(assetTypeProperties.getName());

        return pricingItemsResponse;
    }


    /**
     * <summary>
     * Find all pricingItems
     * </summary>
     * <remarks>this method is responsible for getting all records in pagination</remarks>
     */
    public Page<PricingItems> findAll(Long pricingConfigurationId, Long assetTypeId,
                                      BigDecimal price, PageRequest pageRequest) {
        GenericSpecification<PricingItems> genericSpecification = new GenericSpecification<>();
        if (pricingConfigurationId != null) {
            genericSpecification.add(new SearchCriteria("pricingConfigurationId", pricingConfigurationId, SearchOperation.EQUAL));
        }

        if (assetTypeId != null) {
            genericSpecification.add(new SearchCriteria("assetTypeId", assetTypeId, SearchOperation.EQUAL));
        }
        if (price != null) {
            genericSpecification.add(new SearchCriteria("price", price, SearchOperation.EQUAL));
        }

        Page<PricingItems> pricingItemss = pricingItemsRepository.findAll(genericSpecification, pageRequest);
        return pricingItemss;
    }


    /**
     * <summary>
     * Enable disenable
     * </summary>
     * <remarks>this method is responsible for enabling and dis enabling a pricingItems</remarks>
     */
    public void enableDisablePricingItem(EnableDisEnableDto request) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PricingItems pricingItems = pricingItemsRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested pricingItems Id does not exist!"));
        pricingItems.setIsActive(request.isActive());
        pricingItems.setUpdatedBy(userCurrent.getId());
        pricingItemsRepository.save(pricingItems);

    }


    public List<PricingItems> getAll(Boolean isActive) {
        List<PricingItems> pricingItemss = pricingItemsRepository.findByIsActive(isActive);
        return pricingItemss;

    }
}
