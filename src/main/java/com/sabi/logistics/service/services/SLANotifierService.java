package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.SLANotifierRequestDto;
import com.sabi.logistics.core.dto.response.SLANotifierResponseDto;
import com.sabi.logistics.core.enums.SlaName;
import com.sabi.logistics.core.models.SLANotifier;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.SLANotifierRepository;
import com.sabi.logistics.service.repositories.SLARepository;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SLANotifierService {
    private final SLANotifierRepository slaNotifierRepository;

    private final Validations validations;

    private final ModelMapper mapper;

    private final SLARepository slaRepository;

    public SLANotifierService(SLANotifierRepository slaNotifierRepository, Validations validations, ModelMapper mapper, SLAService slaService, SLARepository slaRepository) {
        this.slaNotifierRepository = slaNotifierRepository;
        this.validations = validations;
        this.mapper = mapper;
        this.slaRepository = slaRepository;
    }

    public SLANotifierResponseDto createSLANotification(SLANotifierRequestDto slaNotifierRequestDto) {
        this.validations.validateSLANotifier(slaNotifierRequestDto);

        User currentUser = TokenService.getCurrentUserFromSecurityContext();

        SLANotifier slaNotifier = mapper.map(slaNotifierRequestDto, SLANotifier.class);
        slaNotifier.setCreatedBy(currentUser.getId());
        slaNotifier.setIsActive(true);
        slaNotifier = slaNotifierRepository.save(slaNotifier);

        return mapper.map(slaNotifier,SLANotifierResponseDto.class);
    }

    public SLANotifierResponseDto findSingleSLANotifier(Long id){
        SLANotifier slaNotifier = slaNotifierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The requested SLA Notifier id doesn't exist"));
        return mapper.map(this.getSingleSLA(slaNotifier), SLANotifierResponseDto.class);
    }

    public List<SLANotifier> getAllByIsActive(Boolean isActive) {
        List<SLANotifier> slaNotifiers = slaNotifierRepository.findByIsActive(isActive);
        slaNotifiers.forEach(this::getSingleSLA);
        return slaNotifiers;
    }

    public SLANotifier getSingleSLA(SLANotifier slaNotifier) {
        slaNotifier.setSlaName(slaRepository.getOne(slaNotifier.getSlaId()).getSlaName().name());
        return slaNotifier;
    }

    public Page<SLANotifier> searchAll(SlaName slaName, String name, String email, Boolean isActive, Pageable pageable) {
        Page<SLANotifier> slaNotifiers = slaNotifierRepository
                .searchAll(slaName, name, email, isActive, pageable);
        slaNotifiers.getContent().stream().forEach(this::getSingleSLA);
        return slaNotifiers;
    }
}
