package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.TripItemRequestDto;
import com.sabi.logistics.core.dto.response.TripItemResponseDto;
import com.sabi.logistics.core.models.TripItem;
import com.sabi.logistics.service.helper.GenericSpecification;
import com.sabi.logistics.service.helper.SearchCriteria;
import com.sabi.logistics.service.helper.SearchOperation;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.TripItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@SuppressWarnings("All")
@Slf4j
@Service
public class TripItemService {

    private final TripItemRepository repository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;

    public TripItemService(TripItemRepository repository, ModelMapper mapper, ObjectMapper objectMapper, Validations validations) {
        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
    }


    public TripItemResponseDto createTripItem(TripItemRequestDto request) {
        validations.validateTripItem(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        TripItem tripItem = mapper.map(request,TripItem.class);
        TripItem exist = repository.findByTripRequestIdAndThirdPartyProductId(request.getTripRequestId(),request.getThirdPartyProductId());
        if(exist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Trip Item already exist");
        }

        tripItem.setCreatedBy(userCurrent.getId());
        tripItem.setIsActive(true);
        tripItem = repository.save(tripItem);
        log.debug("Create new Trip Item - {}"+ new Gson().toJson(tripItem));
        return mapper.map(tripItem, TripItemResponseDto.class);
    }


    public TripItemResponseDto updateTripItem(TripItemRequestDto request) {
        validations.validateTripItem(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        TripItem tripItem = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Trip Item id does not exist!"));
        mapper.map(request, tripItem);

        tripItem.setUpdatedBy(userCurrent.getId());
        repository.save(tripItem);
        log.debug("Trip Item record updated - {}"+ new Gson().toJson(tripItem));
        return mapper.map(tripItem, TripItemResponseDto.class);
    }



    public TripItemResponseDto findTripItem(Long id){
        TripItem tripItem  = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Trip Item id does not exist!"));
        return mapper.map(tripItem,TripItemResponseDto.class);
    }

    public Page<TripItem> findAll(Long thirdPartyProductId, Long tripRequestId, String productName, PageRequest pageRequest ){

        Page<TripItem> tripItems = repository.findByTripItem(thirdPartyProductId, tripRequestId, productName, pageRequest);
        if(tripItems == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return tripItems;
    }


    public void enableDisable (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        TripItem tripItem  = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Trip Item id does not exist!"));
        tripItem.setIsActive(request.isActive());
        tripItem.setUpdatedBy(userCurrent.getId());
        repository.save(tripItem);

    }



    public List<TripItem> getAll(Boolean isActive){
        List<TripItem> tripItems = repository.findByIsActive(isActive);
        return tripItems;

    }
}
