package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.DropOffRequestDto;
import com.sabi.logistics.core.dto.response.DropOffResponseDto;
import com.sabi.logistics.core.models.DropOff;
import com.sabi.logistics.core.models.DropOffItem;
import com.sabi.logistics.core.models.Order;
import com.sabi.logistics.core.models.OrderItem;
import com.sabi.logistics.service.helper.GenericSpecification;
import com.sabi.logistics.service.helper.SearchCriteria;
import com.sabi.logistics.service.helper.SearchOperation;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.*;
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
public class DropOffService {
    private final DropOffRepository dropOffRepository;
    private final ModelMapper mapper;
    @Autowired
    private Validations validations;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DropOffItemRepository dropOffItemRepository;

    @Autowired
    private TripRequestRepository tripRequestRepository;


    public DropOffService(DropOffRepository dropOffRepository, ModelMapper mapper) {
        this.dropOffRepository = dropOffRepository;
        this.mapper = mapper;
    }

    public DropOffResponseDto createDropOff(DropOffRequestDto request) {
        validations.validateDropOff(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DropOff dropOffExists = dropOffRepository.findByTripRequestIdAndOrderId(request.getTripRequestId(), request.getOrderId());
        if(dropOffExists !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " DropOff already exist");
        }
        Order order = orderRepository.getOne(request.getOrderId());
        DropOff dropOff = mapper.map(request,DropOff.class);
        dropOff.setCreatedBy(userCurrent.getId());
        dropOff.setIsActive(true);
        dropOff.setDeliveryAddress(order.getDeliveryAddress());
        dropOff = dropOffRepository.save(dropOff);
        log.debug("Create new trip item - {}"+ new Gson().toJson(dropOff));
        DropOffResponseDto dropOffResponseDto = mapper.map(dropOff, DropOffResponseDto.class);
        dropOffResponseDto.setDeliveryAddress(order.getDeliveryAddress());
        return dropOffResponseDto;
    }

    public DropOffResponseDto updateDropOff(DropOffRequestDto request) {
        validations.validateDropOff(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DropOff dropOff = dropOffRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested DropOff Id does not exist!"));
        Order order = orderRepository.getOne(request.getOrderId());
        mapper.map(request, dropOff);
        dropOff.setUpdatedBy(userCurrent.getId());
        dropOff.setDeliveryAddress(order.getDeliveryAddress());
        dropOffRepository.save(dropOff);
        log.debug("color record updated - {}"+ new Gson().toJson(dropOff));
        DropOffResponseDto dropOffResponseDto = mapper.map(dropOff, DropOffResponseDto.class);
        dropOffResponseDto.setDeliveryAddress(order.getDeliveryAddress());
        return dropOffResponseDto;
    }

    public DropOffResponseDto findDropOff(Long id){
        DropOff dropOff  = dropOffRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested DropOff Id does not exist!"));
        DropOffResponseDto dropOffResponseDto = mapper.map(dropOff, DropOffResponseDto.class);
        Order order = orderRepository.getOne(dropOff.getOrderId());
        dropOffResponseDto.setDropOffItem(getAllDropOffItems(id));
        dropOffResponseDto.setCustomerName(order.getCustomerName());
        dropOffResponseDto.setCustomerPhone(order.getCustomerPhone());

        return dropOffResponseDto;
    }


    public Page<DropOff> findAll(Long orderId, Long tripRequestId, PageRequest pageRequest ){

        GenericSpecification<DropOff> genericSpecification = new GenericSpecification<DropOff>();

        if (orderId != null)
        {
            genericSpecification.add(new SearchCriteria("orderId", orderId, SearchOperation.EQUAL));
        }

        if (tripRequestId != null)
        {
            genericSpecification.add(new SearchCriteria("tripRequestId", tripRequestId, SearchOperation.EQUAL));
        }



        Page<DropOff> dropOffs = dropOffRepository.findAll(genericSpecification,pageRequest);
        if(dropOffs == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }



        return dropOffs;

    }



    public void enableDisable (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DropOff dropOff  = dropOffRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested DropOff Id does not exist!"));
        dropOff.setIsActive(request.isActive());
        dropOff.setUpdatedBy(userCurrent.getId());
        dropOffRepository.save(dropOff);

    }


    public List<DropOff> getAll(Boolean isActive, Long tripRequestId){
        List<DropOff> tripItems = dropOffRepository.findByIsActiveAndTripRequestId(isActive, tripRequestId);

        return tripItems;

    }

    public List<DropOffItem> getAllDropOffItems(Long dropOffId){
        List<DropOffItem> dropOffItems = dropOffItemRepository.findByDropOffId(dropOffId);

        for (DropOffItem dropOffItem : dropOffItems) {
            OrderItem orderItem = orderItemRepository.getOne(dropOffItem.getOrderItemId());
            Order order = orderRepository.getOne(orderItem.getOrderId());
            dropOffItem.setCustomerName(order.getCustomerName());
            dropOffItem.setCustomerPhone(order.getCustomerPhone());
        }
        return dropOffItems;
    }
}
