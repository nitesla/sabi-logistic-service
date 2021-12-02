package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.LGADto;
import com.sabi.logistics.core.dto.response.LGAResponseDto;
import com.sabi.logistics.core.models.LGA;
import com.sabi.logistics.core.models.State;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.LGARepository;
import com.sabi.logistics.service.repositories.StateRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;


@SuppressWarnings("ALL")
@Slf4j
@Service
public class LGAService {

    private LGARepository lgaRepository;
    private StateRepository stateRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;

    public LGAService(LGARepository lgaRepository,StateRepository stateRepository, ModelMapper mapper, ObjectMapper objectMapper,
                      Validations validations) {
        this.lgaRepository = lgaRepository;
        this.stateRepository = stateRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
    }


    /** <summary>
     * LGA creation
     * </summary>
     * <remarks>this method is responsible for creation of new LGA</remarks>
     */

    public LGAResponseDto createLga(LGADto request) {
        validations.validateLGA(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        LGA lga = mapper.map(request,LGA.class);
        LGA lgaExist = lgaRepository.findByName(request.getName());
        if(lgaExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " LGA already exist");
        }
        lga.setCreatedBy(userCurrent.getId());
        lga.setIsActive(true);
        lga = lgaRepository.save(lga);
        log.debug("Create new LGA - {}"+ new Gson().toJson(lga));
        return mapper.map(lga, LGAResponseDto.class);
    }



    /** <summary>
     * LGA update
     * </summary>
     * <remarks>this method is responsible for updating already existing LGA</remarks>
     */

    public LGAResponseDto updateLga(LGADto request) {
        validations.validateLGA(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        LGA lga = lgaRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested LGA Id does not exist!"));
        mapper.map(request, lga);
        lga.setUpdatedBy(userCurrent.getId());
        lgaRepository.save(lga);
        log.debug("LGA record updated - {}" + new Gson().toJson(lga));
        return mapper.map(lga, LGAResponseDto.class);
    }





    /** <summary>
     * Find LGA
     * </summary>
     * <remarks>this method is responsible for getting a single record</remarks>
     */
    public LGAResponseDto findLga(Long id){
        LGA lga = lgaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested LGA Id does not exist!"));
        State state = stateRepository.findById(lga.getStateId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " State Id does not exist!"));
        LGAResponseDto response = LGAResponseDto.builder()
                .id(lga.getId())
                .name(lga.getName())
                .stateId(lga.getStateId())
                .stateName(state.getName())
                .createdDate(lga.getCreatedDate())
                .createdBy(lga.getCreatedBy())
                .updatedBy(lga.getUpdatedBy())
                .updatedDate(lga.getUpdatedDate())
                .isActive(lga.getIsActive())
                .build();
        return response;
    }



    /** <summary>
     * Find all LGA
     * </summary>
     * <remarks>this method is responsible for getting all records in pagination</remarks>
     */




    public Page<LGA> findAll(String name, Long stateId, PageRequest pageRequest ) {
        Page<LGA> lga = lgaRepository.findLgas(name,stateId, pageRequest);
        if (lga == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        lga.getContent().forEach(lgas -> {
            State state = stateRepository.getOne(lgas.getStateId());

            lgas.setStateName(state.getName());
        });
        return lga;

    }


    /** <summary>
     * Enable disenable
     * </summary>
     * <remarks>this method is responsible for enabling and dis enabling a country</remarks>
     */
    public void enableDisEnableState (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        LGA lga = lgaRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested LGA Id does not exist!"));
        lga.setIsActive(request.isActive());
        lga.setUpdatedBy(userCurrent.getId());
        lgaRepository.save(lga);

    }


    public List<LGA> getAllByStateId(Long stateId){
        List<LGA> lga = lgaRepository.findByStateId(stateId);
        for (LGA tran : lga
        ) {
            State state = stateRepository.getOne(tran.getStateId());
            tran.setStateName(state.getName());
        }
        return lga;

    }

    public List<LGA> getAll(Boolean isActive){
        List<LGA> Colors = lgaRepository.findByIsActive(isActive);
        return Colors;

    }
}
