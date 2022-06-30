package com.sabi.logistics.service.services;

import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.BadRequestException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.SLARequestDto;
import com.sabi.logistics.core.dto.response.SLAResponseDto;
import com.sabi.logistics.core.enums.SlaName;
import com.sabi.logistics.core.models.SLA;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.SLARepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SLAService {

    private final SLARepository slaRepository;

    private final Validations validations;

    private final ModelMapper mapper;

    private final UserRepository userRepository;


    public SLAService(SLARepository slaRepository, Validations validations, ModelMapper mapper, UserRepository userRepository) {
        this.slaRepository = slaRepository;
        this.validations = validations;
        this.mapper = mapper;
        this.userRepository = userRepository;
    }

    public SLAResponseDto createSLA(SLARequestDto slaRequestDto) {
        this.validations.validateSLA(slaRequestDto);

        SLA sla = slaRepository.findBySlaName(slaRequestDto.getSlaName());
        if (sla != null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST,"Ooops, this SLA already exists");

        User currentUser = TokenService.getCurrentUserFromSecurityContext();

        sla =  mapper.map(slaRequestDto, SLA.class);
        sla.setCreatedBy(currentUser.getId());
        sla.setIsActive(true);

        sla = slaRepository.save(sla);

        log.info("Successfully created an sla -> {}",sla);

        return mapper.map(sla,SLAResponseDto.class);
    }

    public SLAResponseDto updateSLA(SLARequestDto slaRequestDto){
        this.validations.validateSLA(slaRequestDto);

        SLA sla = slaRepository.findById(slaRequestDto.getId())
                .orElseThrow(()-> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The submitted SLA id doesn't exist"));

        User currentUser = TokenService.getCurrentUserFromSecurityContext();

        mapper.map(slaRequestDto, sla);
        sla.setUpdatedBy(currentUser.getId());
        sla = slaRepository.save(sla);
        log.info("Successfully updated an sla -> {}", sla);

        return mapper.map(sla, SLAResponseDto.class);
    }

    public SLAResponseDto findSingleSLA(Long id) {
        SLA sla = slaRepository.findById(id)
                .orElseThrow(()->new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The requested SLA id does not exist"));
        return mapper.map(sla, SLAResponseDto.class);
    }

    public SLAResponseDto findSLAByName(SlaName slaName) {
        SLA sla = slaRepository.findBySlaName(slaName);
        if (sla == null)
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The requested SLA name does not exist");
        return mapper.map(sla, SLAResponseDto.class);
    }

    public List<SLA> findAllSLAByIsActive(Boolean isActive) {
        return slaRepository.findByIsActive(isActive);
    }

    public Page<SLA> searchAllSLA(Long slaDuration,SlaName slaName, Long triggerDuration, Boolean isActive, Pageable pageable) {
        return slaRepository
                .findAllBySlaDurationAndSlaNameAndTriggerDurationAndIsActive(slaDuration,slaName,triggerDuration,isActive,pageable);
    }

    public void enableDisable(EnableDisEnableDto enableDisEnableDto) {
        SLA sla = slaRepository.findById(enableDisEnableDto.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "The SLA with the given Id cannot be found"));
        sla.setIsActive(enableDisEnableDto.getIsActive());
        sla = slaRepository.save(sla);
    }
}
