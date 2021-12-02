package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.StateDto;
import com.sabi.logistics.core.dto.response.StateResponseDto;
import com.sabi.logistics.core.models.Color;
import com.sabi.logistics.core.models.Country;
import com.sabi.logistics.core.models.State;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.CountryRepository;
import com.sabi.logistics.service.repositories.StateRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 *
 * This class is responsible for all business logic for state
 */


@Slf4j
@Service
public class StateService {


    private CountryRepository countryRepository;
    private StateRepository stateRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;

    public StateService(CountryRepository countryRepository,StateRepository stateRepository, ModelMapper mapper, ObjectMapper objectMapper,Validations validations) {
        this.countryRepository = countryRepository;
        this.stateRepository = stateRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;

    }

    /** <summary>
      * State creation
      * </summary>
      * <remarks>this method is responsible for creation of new states</remarks>
      */

    public StateResponseDto createState(StateDto request) {
        validations.validateState(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        State state = mapper.map(request,State.class);
        State stateExist = stateRepository.findByName(request.getName());
        if(stateExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " State already exist");
        }
        state.setCreatedBy(userCurrent.getId());
        state.setIsActive(true);
        state = stateRepository.save(state);
        log.debug("Create new State - {}"+ new Gson().toJson(state));
        return mapper.map(state, StateResponseDto.class);
    }


    /** <summary>
     * State update
     * </summary>
     * <remarks>this method is responsible for updating already existing states</remarks>
     */

    public StateResponseDto updateState(StateDto request) {
        validations.validateState(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        State state = stateRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested State Id does not exist!"));
        mapper.map(request, state);
        state.setUpdatedBy(userCurrent.getId());
        stateRepository.save(state);
        log.debug("State record updated - {}"+ new Gson().toJson(state));
        return mapper.map(state, StateResponseDto.class);
    }


    /** <summary>
     * Find State
     * </summary>
     * <remarks>this method is responsible for getting a single record</remarks>
     */
    public StateResponseDto findState(Long id){
        State state = stateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested State Id does not exist!"));
        Country country = countryRepository.getOne(state.getCountryId());
        state.setCountryName(country.getName());
        return mapper.map(state,StateResponseDto.class);
    }


    /** <summary>
     * Find all State
     * </summary>
     * <remarks>this method is responsible for getting all records in pagination</remarks>
     */
    public Page<State> findAll(String name,Long countryId, PageRequest pageRequest ){
        Page<State> state = stateRepository.findStates(name,countryId,pageRequest);
            if(state == null){
                throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
            }
        state.getContent().forEach(states -> {
            Country country = countryRepository.getOne(states.getCountryId());

            states.setCountryName(country.getName());
        });
            return state;
    }


    /** <summary>
     * Enable disenable
     * </summary>
     * <remarks>this method is responsible for enabling and dis enabling a state</remarks>
     */
    public void enableDisEnableState (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        State state = stateRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested State Id does not exist!"));
        state.setIsActive(request.isActive());
        state.setUpdatedBy(userCurrent.getId());
        stateRepository.save(state);

    }


    public List<State> getAllByCountryId(Long countryId){
        List<State> states = stateRepository.findByCountryId(countryId);
        for (State tran : states
                ) {
            Country country = countryRepository.getOne(tran.getCountryId());
            tran.setCountryName(country.getName());
        }
        return states;

    }

    public List<State> getAll(Boolean isActive){
        List<State> Colors = stateRepository.findByIsActive(isActive);
        return Colors;

    }


}
