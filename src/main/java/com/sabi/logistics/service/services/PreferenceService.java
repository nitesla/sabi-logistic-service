package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.PreferenceDto;
import com.sabi.logistics.core.dto.response.PreferenceResponseDto;
import com.sabi.logistics.core.models.Partner;
import com.sabi.logistics.core.models.Preference;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.PartnerRepository;
import com.sabi.logistics.service.repositories.PreferenceRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
@SuppressWarnings("ALL")
@Slf4j
@Service
public class PreferenceService {

    private final PartnerRepository partnerRepository;
    private final ModelMapper mapper;
    private final Validations validations;
    private final PreferenceRepository repository;

    public PreferenceService(PartnerRepository partnerRepository, ModelMapper mapper,Validations validations,
                             PreferenceRepository repository) {
        this.partnerRepository = partnerRepository;
        this.mapper = mapper;
        this.validations = validations;
        this.repository = repository;
    }



    public PreferenceResponseDto createPreference(PreferenceDto request) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Preference preference = mapper.map(request,Preference.class);
        Preference productExists = repository.findByPartnerId(request.getPartnerId());
        if(productExists != null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Preference already exist");
        }
        Partner savedPartner = partnerRepository.findPartnerById(request.getPartnerId());
        if (savedPartner == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested partner Id does not exist!");
        }
        preference.setCreatedBy(userCurrent.getId());
        preference.setIsActive(true);
        preference = repository.save(preference);
        log.debug("Create new preference - {}"+ new Gson().toJson(preference));
        PreferenceResponseDto productResponseDto =  mapper.map(preference, PreferenceResponseDto.class);
        return productResponseDto;

    }

    public PreferenceResponseDto updatePreference(PreferenceDto request) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Preference preference = repository.findByPartnerId(request.getPartnerId());
        if (preference == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested preference for partner Id does not exist!");
        }
        mapper.map(request, preference);

        preference.setUpdatedBy(userCurrent.getId());
        preference.setIsActive(true);
        repository.save(preference);
        log.debug("preference record updated - {}"+ new Gson().toJson(preference));
        PreferenceResponseDto preferenceResponseDto =  mapper.map(preference, PreferenceResponseDto.class);
        return preferenceResponseDto;

    }

    public Preference findPreferenceByPartnerId(Long id){
        Preference preference  = repository.findByPartnerId(id);
        return preference;
    }
}
