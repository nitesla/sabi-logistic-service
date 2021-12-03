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
import com.sabi.logistics.core.models.Order;
import com.sabi.logistics.core.models.OrderItem;
import com.sabi.logistics.core.models.TripItem;
import com.sabi.logistics.core.models.TripRequest;
import com.sabi.logistics.service.helper.GenericSpecification;
import com.sabi.logistics.service.helper.SearchCriteria;
import com.sabi.logistics.service.helper.SearchOperation;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.OrderItemRepository;
import com.sabi.logistics.service.repositories.OrderRepository;
import com.sabi.logistics.service.repositories.TripItemRepository;
import com.sabi.logistics.service.repositories.TripRequestRepository;
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

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TripRequestRepository tripRequestRepository;


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

        Order order = orderRepository.getOne(orderItem.getOrderID());


        tripItem.setCreatedBy(userCurrent.getId());
        tripItem.setIsActive(true);
        tripItem = tripItemRepository.save(tripItem);
        log.debug("Create new trip item - {}"+ new Gson().toJson(tripItem));

        TripItemResponseDto tripItemResponseDto = mapper.map(tripItem, TripItemResponseDto.class);
        tripItemResponseDto.setOrderItemName(orderItem.getName());
        tripItemResponseDto.setQty(orderItem.getQty());
        tripItemResponseDto.setDeliveryAddress(order.getDeliveryAddress());
        tripItemResponseDto.setCustomerName(order.getCustomerName());
        tripItemResponseDto.setCustomerPhone(order.getCustomerPhone());
        tripItemResponseDto.setOrderId(orderItem.getOrderID());

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
            Order order = orderRepository.getOne(orderItem.getOrderID());
            tripItemResponseDto.setOrderItemName(orderItem.getName());
            tripItemResponseDto.setQty(orderItem.getQty());
            tripItemResponseDto.setDeliveryAddress(order.getDeliveryAddress());
            tripItemResponseDto.setCustomerName(order.getCustomerName());
            tripItemResponseDto.setCustomerPhone(order.getCustomerPhone());
            tripItemResponseDto.setOrderId(orderItem.getOrderID());
        }

        return tripItemResponseDto;
    }

    public TripItemResponseDto findTripItem(Long id){
        TripItem tripItem  = tripItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested trip item Id does not exist!"));
        TripItemResponseDto tripItemResponseDto = mapper.map(tripItem, TripItemResponseDto.class);
        OrderItem orderItem = orderItemRepository.getOne(tripItem.getOrderItemID());

        Order order = orderRepository.getOne(orderItem.getOrderID());

        tripItemResponseDto.setOrderItemName(orderItem.getName());
        tripItemResponseDto.setQty(orderItem.getQty());
        tripItemResponseDto.setDeliveryAddress(order.getDeliveryAddress());
        tripItemResponseDto.setCustomerName(order.getCustomerName());
        tripItemResponseDto.setCustomerPhone(order.getCustomerPhone());
        tripItemResponseDto.setOrderId(orderItem.getOrderID());

        return tripItemResponseDto;
    }


    public Page<TripItem> findAll(Long orderItemID, Long tripRequestID,
                                   String status, PageRequest pageRequest ){

        GenericSpecification<TripItem> genericSpecification = new GenericSpecification<TripItem>();

        if (orderItemID != null)
        {
            genericSpecification.add(new SearchCriteria("orderItemID", orderItemID, SearchOperation.EQUAL));
        }

        if (tripRequestID != null)
        {
            genericSpecification.add(new SearchCriteria("tripRequestID", tripRequestID, SearchOperation.EQUAL));
        }

        if (status != null && !status.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("status", status, SearchOperation.MATCH));
        }



        Page<TripItem> tripItems = tripItemRepository.findAll(genericSpecification,pageRequest);
        if(tripItems == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        tripItems.getContent().forEach(item ->{
            TripItem tripItem = tripItemRepository.getOne(item.getId());


            OrderItem orderItem = orderItemRepository.getOne(tripItem.getOrderItemID());

            Order order = orderRepository.getOne(orderItem.getOrderID());

            item.setOrderItemName(orderItem.getName());
            item.setQty(orderItem.getQty());
            item.setDeliveryAddress(order.getDeliveryAddress());
            item.setCustomerName(order.getCustomerName());
            item.setCustomerPhone(order.getCustomerPhone());
            item.setOrderId(orderItem.getOrderID());

        });



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

        for (TripItem item : tripItems) {
            OrderItem orderItem = orderItemRepository.getOne(item.getOrderItemID());

            Order order = orderRepository.getOne(orderItem.getOrderID());

            item.setOrderItemName(orderItem.getName());
            item.setQty(orderItem.getQty());
            item.setDeliveryAddress(order.getDeliveryAddress());
            item.setCustomerName(order.getCustomerName());
            item.setCustomerPhone(order.getCustomerPhone());
            item.setOrderId(orderItem.getOrderID());

        }
        return tripItems;

    }

    public List<TripItem> getInvoice(Long tripRequestID, Long orderID){

        List<TripItem> tripItems = tripItemRepository.findByTripRequestIDAndOrderID(tripRequestID, orderID);

        for (TripItem item : tripItems) {
            OrderItem orderItem = orderItemRepository.getOne(item.getOrderItemID());

            TripRequest tripRequest = tripRequestRepository.findById(tripRequestID)
                    .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested tripRequestId does not exist!"));

            Order order = orderRepository.getOne(orderItem.getOrderID());

            item.setOrderItemName(orderItem.getName());
            item.setQty(orderItem.getQty());
            item.setDeliveryAddress(order.getDeliveryAddress());
            item.setCustomerName(order.getCustomerName());
            item.setCustomerPhone(order.getCustomerPhone());
            item.setOrderId(orderItem.getOrderID());
            item.setDeliveryDate(tripRequest.getDateDelivered());
            item.setTax(order.getTax());
            item.setReferenceNo(tripRequest.getReferenceNo());
        }
        return tripItems;

    }
}
