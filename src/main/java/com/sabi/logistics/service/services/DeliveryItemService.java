package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.DeliveryItemRequestDto;
import com.sabi.logistics.core.dto.response.DeliveryItemResponseDto;
import com.sabi.logistics.core.models.DeliveryItem;
import com.sabi.logistics.service.helper.GenericSpecification;
import com.sabi.logistics.service.helper.SearchCriteria;
import com.sabi.logistics.service.helper.SearchOperation;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.DeliveryItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DeliveryItemService {
    private final DeliveryItemRepository deliveryItemRepository;
    private final ModelMapper mapper;
    @Autowired
    private Validations validations;


    public DeliveryItemService(DeliveryItemRepository deliveryItemRepository, ModelMapper mapper) {
        this.deliveryItemRepository = deliveryItemRepository;
        this.mapper = mapper;
    }

    public DeliveryItemResponseDto createDeliveryItem(DeliveryItemRequestDto request) {
        validations.validateDeliveryItem(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DeliveryItem deliveryItem = mapper.map(request,DeliveryItem.class);

        DeliveryItem deliveryItemExists = deliveryItemRepository.findByDeliveryIDAndTripRequestID(deliveryItem.getDeliveryID(), deliveryItem.getTripRequestID());
        

        if(deliveryItemExists !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Delivery Item already exist");
        }
        deliveryItem.setCreatedBy(userCurrent.getId());
        deliveryItem.setIsActive(true);
        deliveryItem = deliveryItemRepository.save(deliveryItem);
        log.debug("Create new delivery item - {}"+ new Gson().toJson(deliveryItem));
        return mapper.map(deliveryItem, DeliveryItemResponseDto.class);
    }

    public DeliveryItemResponseDto updateDeliveryItem(DeliveryItemRequestDto request) {
        validations.validateDeliveryItem(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DeliveryItem deliveryItem = deliveryItemRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested delivery item Id does not exist!"));
        mapper.map(request, deliveryItem);
        deliveryItem.setUpdatedBy(userCurrent.getId());
        deliveryItemRepository.save(deliveryItem);
        log.debug("color record updated - {}"+ new Gson().toJson(deliveryItem));
        return mapper.map(deliveryItem, DeliveryItemResponseDto.class);
    }

    public DeliveryItemResponseDto findDeliveryItem(Long id){
        DeliveryItem deliveryItem  = deliveryItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested delivery item Id does not exist!"));
        return mapper.map(deliveryItem, DeliveryItemResponseDto.class);
    }


    public Page<DeliveryItem> findAll(Long deliveryID, Long tripRequestID,
                                   String status, PageRequest pageRequest ){

        GenericSpecification<DeliveryItem> genericSpecification = new GenericSpecification<DeliveryItem>();

        if (deliveryID != null)
        {
            genericSpecification.add(new SearchCriteria("deliveryID", deliveryID, SearchOperation.EQUAL));
        }

        if (tripRequestID != null)
        {
            genericSpecification.add(new SearchCriteria("tripRequestID", tripRequestID, SearchOperation.EQUAL));
        }

        if (status != null && !status.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("status", status, SearchOperation.MATCH));
        }



        Page<DeliveryItem> deliveryItems = deliveryItemRepository.findAll(genericSpecification,pageRequest);
        if(deliveryItems == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return deliveryItems;

    }



    public void enableDisable (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DeliveryItem deliveryItem  = deliveryItemRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Delivery item Id does not exist!"));
        deliveryItem.setIsActive(request.isActive());
        deliveryItem.setUpdatedBy(userCurrent.getId());
        deliveryItemRepository.save(deliveryItem);

    }


    public List<DeliveryItem> getAll(Boolean isActive){
        List<DeliveryItem> deliveryItems = deliveryItemRepository.findByIsActive(isActive);
        return deliveryItems;

    }
}
