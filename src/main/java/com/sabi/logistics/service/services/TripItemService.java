package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.TripItemRequestDto;
import com.sabi.logistics.core.dto.response.TripItemResponseDto;
import com.sabi.logistics.core.models.OrderItem;
import com.sabi.logistics.core.models.TripItem;
import com.sabi.logistics.service.helper.GenericSpecification;
import com.sabi.logistics.service.helper.SearchCriteria;
import com.sabi.logistics.service.helper.SearchOperation;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.OrderItemRepository;
import com.sabi.logistics.service.repositories.TripItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;


@SuppressWarnings("All")
@Service
@Slf4j
public class TripItemService {
    private final TripItemRepository tripItemRepository;
    private final ModelMapper mapper;
    @Autowired
    private Validations validations;

    @Autowired
    private OrderItemRepository orderItemRepository;


    public TripItemService(TripItemRepository tripItemRepository, ModelMapper mapper) {
        this.tripItemRepository = tripItemRepository;
        this.mapper = mapper;
    }

    public TripItemResponseDto createTripItem(TripItemRequestDto request) {
        validations.validateTripItem(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        TripItem tripItem = mapper.map(request,TripItem.class);

        TripItem tripItemExists = tripItemRepository.findByOrderItemIDAndTripRequestID(request.getOrderItemID(), request.getTripRequestID());


        if(tripItemExists !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Trip Item already exist");
        }

        OrderItem orderItem = orderItemRepository.getOne(request.getOrderItemID());


        tripItem.setCreatedBy(userCurrent.getId());
        tripItem.setIsActive(true);
        tripItem = tripItemRepository.save(tripItem);
        log.debug("Create new trip item - {}"+ new Gson().toJson(tripItem));

        TripItemResponseDto tripItemResponseDto = mapper.map(tripItem, TripItemResponseDto.class);
        tripItemResponseDto.setOrderItemName(orderItem.getName());

        return tripItemResponseDto;

    }

    public TripItemResponseDto updateTripItem(TripItemRequestDto request) {
        validations.validateTripItem(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        TripItem tripItem = tripItemRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested trip item Id does not exist!"));
        mapper.map(request, tripItem);
        tripItem.setUpdatedBy(userCurrent.getId());
        tripItemRepository.save(tripItem);
        log.debug("color record updated - {}"+ new Gson().toJson(tripItem));
        TripItemResponseDto tripItemResponseDto = mapper.map(tripItem, TripItemResponseDto.class);

        if(request.getOrderItemID() != null ) {
            OrderItem orderItem = orderItemRepository.getOne(request.getOrderItemID());
            tripItemResponseDto.setOrderItemName(orderItem.getName());
        }

        return tripItemResponseDto;
    }

    public TripItemResponseDto findTripItem(Long id){
        TripItem tripItem  = tripItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested trip item Id does not exist!"));
        return mapper.map(tripItem, TripItemResponseDto.class);
    }


    public Page<TripItem> findAll(Long orderItemID, Long tripRequest,
                                   String status, PageRequest pageRequest ){

        GenericSpecification<TripItem> genericSpecification = new GenericSpecification<TripItem>();

        if (orderItemID != null)
        {
            genericSpecification.add(new SearchCriteria("orderItemID", orderItemID, SearchOperation.EQUAL));
        }

        if (tripRequest != null)
        {
            genericSpecification.add(new SearchCriteria("tripRequest", tripRequest, SearchOperation.EQUAL));
        }

        if (status != null && !status.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("status", status, SearchOperation.MATCH));
        }



        Page<TripItem> tripItems = tripItemRepository.findAll(genericSpecification,pageRequest);
        if(tripItems == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return tripItems;

    }



    public void enableDisable (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        TripItem tripItem  = tripItemRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Trip item Id does not exist!"));
        tripItem.setIsActive(request.isActive());
        tripItem.setUpdatedBy(userCurrent.getId());
        tripItemRepository.save(tripItem);

    }


    public List<TripItem> getAll(Boolean isActive){
        List<TripItem> tripItems = tripItemRepository.findByIsActive(isActive);
        return tripItems;

    }
}
