package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.PartnerUserRequestDto;
import com.sabi.logistics.core.dto.response.PartnerUserResponseDto;
import com.sabi.logistics.core.models.PartnerUser;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.PartnerUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PartnerUserService {
    private final PartnerUserRepository partnerUserRepository;
    private final ModelMapper mapper;
    private final Validations validations;

    public PartnerUserService(PartnerUserRepository PartnerUserRepository, ModelMapper mapper, Validations validations) {
        this.partnerUserRepository = PartnerUserRepository;
        this.mapper = mapper;
        this.validations = validations;
    }

    public PartnerUserResponseDto createPartnerUser(PartnerUserRequestDto request) {
        validations.validatePartnerUser(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerUser partnerUser = mapper.map(request,PartnerUser.class);
        boolean partnerUserExists = partnerUserRepository.existsByPartnerIdAndUserId(request.getPartnerId(), request.getUserId());
        if(partnerUserExists){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " PartnerUser already exist");
        }
        partnerUser.setCreatedBy(userCurrent.getId());
        partnerUser.setActive(true);
        partnerUser = partnerUserRepository.save(partnerUser);
        log.debug("Create new PartnerUser - {}"+ new Gson().toJson(partnerUser));
        return mapper.map(partnerUser, PartnerUserResponseDto.class);
    }

    public PartnerUserResponseDto updatePartnerUser(PartnerUserRequestDto request) {
        validations.validatePartnerUser(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerUser partnerUser = partnerUserRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested PartnerUser Id does not exist!"));
        mapper.map(request, partnerUser);
        partnerUser.setUpdatedBy(userCurrent.getId());
        partnerUserRepository.save(partnerUser);
        log.debug("PartnerUser record updated - {}"+ new Gson().toJson(partnerUser));
        return mapper.map(partnerUser, PartnerUserResponseDto.class);
    }

    public PartnerUserResponseDto findPartnerUser(Long id){
        PartnerUser partnerUser  = partnerUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested PartnerUser Id does not exist!"));
        return mapper.map(partnerUser, PartnerUserResponseDto.class);
    }


    public Page<PartnerUser> findAll(long partnerId, PageRequest pageRequest ){
        Page<PartnerUser> PartnerUsers = partnerUserRepository.findPartnerUser(partnerId,pageRequest);
        if(PartnerUsers == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return PartnerUsers;

    }



    public void enableDisEnableState (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerUser PartnerUser  = partnerUserRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested PartnerUser Id does not exist!"));
        PartnerUser.setActive(request.isActive());
        PartnerUser.setUpdatedBy(userCurrent.getId());
        partnerUserRepository.save(PartnerUser);

    }


    public List<PartnerUser> getAll(Boolean isActive){
        List<PartnerUser> PartnerUsers = partnerUserRepository.findByIsActive(isActive);
        return PartnerUsers;

    }
}
