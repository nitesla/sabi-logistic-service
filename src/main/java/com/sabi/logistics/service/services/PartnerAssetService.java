package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.PartnerAssetRequestDto;
import com.sabi.logistics.core.dto.response.PartnerAssetResponseDto;
import com.sabi.logistics.core.models.PartnerAsset;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.PartnerAssetRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PartnerAssetService {
    private final PartnerAssetRepository partnerAssetRepository;
    private final ModelMapper mapper;
    private final Validations validations;

    public PartnerAssetService(PartnerAssetRepository PartnerAssetRepository, ModelMapper mapper, Validations validations) {
        this.partnerAssetRepository = PartnerAssetRepository;
        this.mapper = mapper;
        this.validations = validations;
    }

    public PartnerAssetResponseDto createPartnerAsset(PartnerAssetRequestDto request) {
        validations.validatePartnerAsset(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerAsset partnerAsset = mapper.map(request,PartnerAsset.class);
        PartnerAsset PartnerAssetExists = partnerAssetRepository.findByPlateNo(request.getPlateNo());
        if(PartnerAssetExists !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " partnerAsset already exist");
        }
        partnerAsset.setCreatedBy(userCurrent.getId());
        partnerAsset.setActive(true);
        partnerAsset = partnerAssetRepository.save(partnerAsset);
        log.debug("Create new PartnerAsset - {}"+ new Gson().toJson(partnerAsset));
        return mapper.map(partnerAsset, PartnerAssetResponseDto.class);
    }

    public PartnerAssetResponseDto updatePartnerAsset(PartnerAssetRequestDto request) {
        validations.validatePartnerAsset(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerAsset partnerAsset = partnerAssetRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partnerAsset Id does not exist!"));
        mapper.map(request, partnerAsset);
        partnerAsset.setUpdatedBy(userCurrent.getId());
        partnerAssetRepository.save(partnerAsset);
        log.debug("partnerAsset record updated - {}"+ new Gson().toJson(partnerAsset));
        return mapper.map(partnerAsset, PartnerAssetResponseDto.class);
    }

    public PartnerAssetResponseDto findPartnerAsset(Long id){
        PartnerAsset partnerAsset  = partnerAssetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested PartnerAsset Id does not exist!"));
        return mapper.map(partnerAsset, PartnerAssetResponseDto.class);
    }


    public Page<PartnerAsset> findAll(String name, PageRequest pageRequest ){
        Page<PartnerAsset> PartnerAssets = partnerAssetRepository.findPartnerAsset(name,pageRequest);
        if(PartnerAssets == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return PartnerAssets;

    }



    public void enableDisEnableState (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerAsset partnerAsset  = partnerAssetRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested PartnerAsset Id does not exist!"));
        partnerAsset.setActive(request.isActive());
        partnerAsset.setUpdatedBy(userCurrent.getId());
        partnerAssetRepository.save(partnerAsset);

    }


    public List<PartnerAsset> getAll(Boolean isActive){
        List<PartnerAsset> partnerAssets = partnerAssetRepository.findByIsActive(isActive);
        return partnerAssets;

    }
}
