package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.PartnerAssetTypeRequestDto;
import com.sabi.logistics.core.dto.response.PartnerAssetTypeResponseDto;
import com.sabi.logistics.core.models.AssetTypeProperties;
import com.sabi.logistics.core.models.Partner;
import com.sabi.logistics.core.models.PartnerAssetType;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.AssetTypePropertiesRepository;
import com.sabi.logistics.service.repositories.PartnerAssetTypeRepository;
import com.sabi.logistics.service.repositories.PartnerRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PartnerAssetTypeService {
    private PartnerAssetTypeRepository partnerAssetTypeRepository;
    private AssetTypePropertiesRepository assetTypePropertiesRepository;
    private PartnerRepository partnerRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;

    public PartnerAssetTypeService(PartnerAssetTypeRepository partnerAssetTypeRepository, AssetTypePropertiesRepository assetTypePropertiesRepository, PartnerRepository partnerRepository, ModelMapper mapper, ObjectMapper objectMapper, Validations validations) {
        this.partnerAssetTypeRepository = partnerAssetTypeRepository;
        this.assetTypePropertiesRepository = assetTypePropertiesRepository;
        this.partnerRepository = partnerRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
    }

    public PartnerAssetTypeResponseDto createPartnerAssetType(PartnerAssetTypeRequestDto request) {
        validations.validatePartnerAssetType(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerAssetType partnerAssetType = mapper.map(request,PartnerAssetType.class);
        PartnerAssetType partnerAssetTypeExists
                = partnerAssetTypeRepository.findByAssetTypeId(request.getId());
        if(partnerAssetTypeExists !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " PartnerAssetType already exist");
        }
        AssetTypeProperties assetTypeProperties = assetTypePropertiesRepository.getOne(request.getAssetTypeId());
        Partner partner  = partnerRepository.getOne(request.getPartnerId());
        partnerAssetType.setAssetTypeName(assetTypeProperties.getName());
        partnerAssetType.setPartnerName(partner.getName());
        partnerAssetType.setCreatedBy(userCurrent.getId());
        partnerAssetType.setIsActive(true);
        partnerAssetType = partnerAssetTypeRepository.save(partnerAssetType);
        log.debug("Create new PartnerAssetType - {}"+ new Gson().toJson(partnerAssetType));
        return mapper.map(partnerAssetType, PartnerAssetTypeResponseDto.class);
    }

    public PartnerAssetTypeResponseDto updatePartnerAssetType(PartnerAssetTypeRequestDto request) {
        validations.validatePartnerAssetType(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerAssetType partnerAssetType = partnerAssetTypeRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested PartnerAssetType Id does not exist!"));
        mapper.map(request, partnerAssetType);
        partnerAssetType.setUpdatedBy(userCurrent.getId());
        partnerAssetTypeRepository.save(partnerAssetType);
        log.debug("PartnerAssetType record updated - {}"+ new Gson().toJson(partnerAssetType));
        return mapper.map(partnerAssetType, PartnerAssetTypeResponseDto.class);
    }

    public PartnerAssetTypeResponseDto findPartnerAssetType(Long id){
        PartnerAssetType partnerAssetType  = partnerAssetTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested PartnerAssetType Id does not exist!"));
        return mapper.map(partnerAssetType, PartnerAssetTypeResponseDto.class);
    }


    public Page<PartnerAssetType> findAll(long id , PageRequest pageRequest ){
        Page<PartnerAssetType> partnerAssetType = partnerAssetTypeRepository.findPartnerAssetType(id,pageRequest);
        if(partnerAssetType == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return partnerAssetType;

    }



    public void enableDisEnableState (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerAssetType partnerAssetType  = partnerAssetTypeRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested PartnerAssetType Id does not exist!"));
        partnerAssetType.setIsActive(request.isActive());
        partnerAssetType.setUpdatedBy(userCurrent.getId());
        partnerAssetTypeRepository.save(partnerAssetType);

    }


    public List<PartnerAssetType> getAll(Boolean isActive){
        List<PartnerAssetType> partnerAssetTypes = partnerAssetTypeRepository.findByIsActive(isActive);
        return partnerAssetTypes;
    }
}
