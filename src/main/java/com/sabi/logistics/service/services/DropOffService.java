package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.BadRequestException;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.*;
import com.sabi.logistics.core.dto.response.DropOffItemResponseDto;
import com.sabi.logistics.core.dto.response.DropOffResponseDto;
import com.sabi.logistics.core.models.*;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


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

    @Autowired
    private DropOffItemService dropOffItemService;

    @Autowired
    private OrderService orderService;


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
        dropOff.setDeliveryCode(validations.generateReferenceNumber(6));
        dropOff.setCreatedBy(userCurrent.getId());
        dropOff.setIsActive(true);
        dropOff.setFinalDropOff(false);
        dropOff.setReturnStatus("none");
        dropOff.setDeliveryAddress(order.getDeliveryAddress());
        dropOff.setPaymentStatus(order.getPaymentStatus());
        dropOff = dropOffRepository.save(dropOff);
        log.debug("Create new trip item - {}"+ new Gson().toJson(dropOff));
        DropOffResponseDto dropOffResponseDto = mapper.map(dropOff, DropOffResponseDto.class);
        dropOffResponseDto.setDeliveryAddress(order.getDeliveryAddress());
        return dropOffResponseDto;
    }

    public List<DropOffResponseDto> createDropOffs(List<DropOffMasterRequestDto> requests, Long tripRequestId) {
        List<DropOffResponseDto> responseDtos = new ArrayList<>();
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        requests.forEach(request-> {
            List<DropOffItem> dropOffItemResponseDtos = new ArrayList<>();
            request.setTripRequestId(tripRequestId);
            validations.validateDropOffs(request);

            Order order = orderRepository.getOne(request.getOrderId());
            DropOff dropOff = mapper.map(request, DropOff.class);
            dropOff.setDeliveryCode(validations.generateReferenceNumber(6));
            dropOff.setCreatedBy(userCurrent.getId());
            dropOff.setIsActive(true);
            dropOff.setFinalDropOff(false);
            dropOff.setReturnStatus("none");
            dropOff.setDeliveryAddress(order.getDeliveryAddress());
            dropOff.setPaymentStatus(order.getPaymentStatus());
            dropOff = dropOffRepository.save(dropOff);
            log.debug("Create new trip item - {}" + new Gson().toJson(dropOff));
            DropOffResponseDto dropOffResponseDto = mapper.map(dropOff, DropOffResponseDto.class);

            if(request.getDropOffItem() != null) {
                dropOffItemResponseDtos = dropOffItemService.createDropOffItems(request.getDropOffItem(), dropOffResponseDto.getId());
                List<DropOffItem> finalDropOffItemResponse = dropOffItemResponseDtos;
                dropOffItemResponseDtos.forEach(itemResponse -> {
                    dropOffResponseDto.setDropOffItem(finalDropOffItemResponse);
                });
            }

            responseDtos.add(dropOffResponseDto);
        });
        return responseDtos;
    }

    public DropOffResponseDto updateDropOff(DropOffRequestDto request) {
        validations.validateDropOff(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DropOff dropOff = dropOffRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested DropOff Id does not exist!"));
        Order order = orderRepository.getOne(request.getOrderId());
        mapper.map(request, dropOff);
        if (dropOff.getDeliveryStatus().equalsIgnoreCase("completed")){
            dropOff.setReturnStatus("none");
        } else if (dropOff.getDeliveryStatus().equalsIgnoreCase("PartiallyCompleted") || dropOff.getDeliveryStatus().equalsIgnoreCase("failed")){
            dropOff.setReturnStatus("pending");
        } else if (dropOff.getDeliveryStatus().equalsIgnoreCase("returned")) {
            dropOff.setReturnStatus("returned");
        }else {
            dropOff.setReturnStatus("none");
        }

        if (dropOff.getPaymentStatus().equalsIgnoreCase("paid")) {
            dropOff.setPaidStatus("paid");
        }
        if (dropOff.getPaymentStatus().equalsIgnoreCase("PayOnDelivery")) {
            dropOff.setPaidStatus("pending");
        }


        dropOff.setUpdatedBy(userCurrent.getId());
        dropOff.setDeliveryAddress(order.getDeliveryAddress());
        dropOff.setPaymentStatus(order.getPaymentStatus());
        dropOffRepository.save(dropOff);
        log.debug("record updated - {}"+ new Gson().toJson(dropOff));
        DropOffResponseDto dropOffResponseDto = mapper.map(dropOff, DropOffResponseDto.class);
        dropOffResponseDto.setDeliveryAddress(order.getDeliveryAddress());
        return dropOffResponseDto;
    }


    public DropOffResponseDto updateDropOffStatus(DropOffStatusDto request) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DropOff dropOff = dropOffRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested DropOff Id does not exist!"));

        if (request.getDeliveryStatus() == null || request.getDeliveryStatus().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Status cannot be empty");
        if (!("pending".equalsIgnoreCase(request.getDeliveryStatus())  || "PartiallyCompleted".equalsIgnoreCase(request.getDeliveryStatus())
                || "cancelled".equalsIgnoreCase(request.getDeliveryStatus()) || "InTransit".equalsIgnoreCase(request.getDeliveryStatus())
                || "failed".equalsIgnoreCase(request.getDeliveryStatus()) || "returned".equalsIgnoreCase(request.getDeliveryStatus())
                || "completed".equalsIgnoreCase(request.getDeliveryStatus())))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Enter the correct delivery Status for dropOff");


        log.info("request {}"+ request.getDeliveryCode());
        log.info("Computer {}" + dropOff.getDeliveryCode());


        if (request.getDeliveryCode() == null || request.getDeliveryCode().isEmpty()) {
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Status cannot be empty");
        }
        if (!request.getDeliveryCode().equalsIgnoreCase(dropOff.getDeliveryCode())){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Invalid Delivery Code");
        }

        if (request.getDeliveryStatus().equalsIgnoreCase("completed") && dropOff.getPaymentStatus().equalsIgnoreCase("PayOnDelivery") && (request.getTotalAmount() != dropOff.getTotalAmount())) {
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Invalid Amount");
        }

        if (request.getDeliveryStatus().equalsIgnoreCase("PartiallyCompleted") && dropOff.getPaymentStatus().equalsIgnoreCase("PayOnDelivery") && request.getTotalAmount().equals(0)) {
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Invalid Amount");
        }

        DropOffItemRequestDto dropOffItemRequestDto = new DropOffItemRequestDto();
        Order order = new Order();
        OrderRequestDto orderRequestDto = new OrderRequestDto();
        OrderItemRequestDto orderItemRequestDto = new OrderItemRequestDto();

        mapper.map(request, dropOff);
        if (dropOff.getDeliveryStatus().equalsIgnoreCase("completed")){
            dropOff.setReturnStatus("none");
        } else if (dropOff.getDeliveryStatus().equalsIgnoreCase("PartiallyCompleted") || dropOff.getDeliveryStatus().equalsIgnoreCase("failed")){
            dropOff.setReturnStatus("pending");
        } else if (dropOff.getDeliveryStatus().equalsIgnoreCase("returned")) {
            dropOff.setReturnStatus("returned");
        }else {
            dropOff.setReturnStatus("none");
        }

        if (dropOff.getPaymentStatus().equalsIgnoreCase("paid")) {
            dropOff.setPaidStatus("paid");
        }
        if (dropOff.getPaymentStatus().equalsIgnoreCase("PayOnDelivery")) {
            dropOff.setPaidStatus("pending");
        }

        dropOff.setUpdatedBy(userCurrent.getId());
        dropOffRepository.save(dropOff);

        DropOffResponseDto dropOffResponseDto = mapper.map(dropOff, DropOffResponseDto.class);


        TripRequest tripRequest = tripRequestRepository.getOne(dropOff.getTripRequestId());

        if(dropOff.getFinalDropOff() == true) {
            tripRequest.setStatus("completed");
        }

        List<DropOff> dropItems = dropOffRepository.findByTripRequestId(dropOff.getTripRequestId());

//                if(dropItems.stream().map(DropOffItem::getStatus).allMatch(response -> dropOffItem.getStatus().equals("completed"))){

        if(dropItems.stream().allMatch(response -> response.getDeliveryStatus().equalsIgnoreCase("completed"))){
            tripRequest.setDeliveryStatus("completed");
        } else if (dropItems.stream().allMatch(response -> response.getDeliveryStatus().equalsIgnoreCase("failed"))){
            tripRequest.setDeliveryStatus("failed");
        } else {
            tripRequest.setDeliveryStatus("PartiallyCompleted");
        }

        tripRequestRepository.save(tripRequest);

        if(request.getDropOffItem() != null) {
            List<DropOffItemResponseDto> dropOffItems = dropOffItemService.updateDropOffItemStatus(request.getDropOffItem(), dropOffResponseDto.getId());
        }

        order = orderRepository.findOrderById(dropOff.getOrderId());
        if (order != null) {
            orderRequestDto.setDeliveryStatus(dropOff.getDeliveryStatus());
            orderRequestDto.setId(dropOff.getOrderId());
            orderService.updateOrderStatus(orderRequestDto);
        }

        log.debug("record updated - {}"+ new Gson().toJson(dropOff));
        return  mapper.map(dropOff, DropOffResponseDto.class);

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
        dropOffResponseDto.setDeliveryAddress(order.getDeliveryAddress());

        if (dropOff.getPaymentStatus() != null && dropOffResponseDto.getPaymentStatus().equalsIgnoreCase("Pay On Delivery")) {
            List<DropOffItem> dropOffItems = dropOffItemRepository.findByDropOffId(id);
            dropOffResponseDto.setTotalAmount(getTotalAmount(dropOffItems));
        }
        return dropOffResponseDto;
    }


    public Page<DropOff> findAll(Long orderId, Long tripRequestId, PageRequest pageRequest ){

        Page<DropOff> dropOffs = dropOffRepository.findDropOff(orderId, tripRequestId,pageRequest);
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

    public DropOffResponseDto updatePaidStatus(String paidStatus, Long dropOffId ){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DropOff dropOff = dropOffRepository.findById(dropOffId)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested DropOff Id does not exist!"));

        dropOff.setPaidStatus(paidStatus);

        dropOff.setUpdatedBy(userCurrent.getId());
        dropOffRepository.save(dropOff);
        log.debug("record updated - {}"+ new Gson().toJson(dropOff));
        return mapper.map(dropOff, DropOffResponseDto.class);

    }

    public DropOffResponseDto updateReturnStatus(String returnStatus, Long dropOffId ){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DropOff dropOff = dropOffRepository.findById(dropOffId)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested DropOff Id does not exist!"));

        dropOff.setReturnStatus(returnStatus);

        dropOff.setUpdatedBy(userCurrent.getId());
        dropOffRepository.save(dropOff);
        log.debug("record updated - {}"+ new Gson().toJson(dropOff));
        return mapper.map(dropOff, DropOffResponseDto.class);

    }

    public List<DropOff> getAllDropOffs(String paidStatus, Long tripRequestId){
        List<DropOff> dropOffs = dropOffRepository.findByTripRequestIdAndPaidStatus(tripRequestId, paidStatus);

        for (DropOff dropOff : dropOffs) {

            Order order = orderRepository.getOne(dropOff.getOrderId());
            dropOff.setCustomerName(order.getCustomerName());
            dropOff.setDeliveryAddress(order.getDeliveryAddress());
            dropOff.setCustomerPhone(order.getCustomerPhone());


            if (dropOff.getPaymentStatus() != null && dropOff.getPaymentStatus().equalsIgnoreCase("PayOnDelivery")) {
                List<DropOffItem> dropOffItems = dropOffItemRepository.findByDropOffId(dropOff.getId());
                dropOff.setTotalAmount(getTotalAmount(dropOffItems));
            }

            dropOff.setDropOffItem(getAllDropOffItems(dropOff.getId()));
        }



        return dropOffs;

    }

    public List<DropOffItem> getAllDropOffItems(Long dropOffId){
        List<DropOffItem> dropOffItems = dropOffItemRepository.findByDropOffId(dropOffId);

        for (DropOffItem dropOffItem : dropOffItems) {
            OrderItem orderItem = orderItemRepository.getOne(dropOffItem.getOrderItemId());
            Order order = orderRepository.getOne(orderItem.getOrderId());
            dropOffItem.setCustomerName(order.getCustomerName());
            dropOffItem.setCustomerPhone(order.getCustomerPhone());
            dropOffItem.setOrderItemName(orderItem.getProductName());
            dropOffItem.setThirdPartyProductId(orderItem.getThirdPartyProductId());
            dropOffItem.setQty(orderItem.getQty());
            dropOffItem.setOrderId(orderItem.getOrderId());
        }
        return dropOffItems;
    }

    private BigDecimal getTotalAmount(List<DropOffItem> dropOffItems) {
        return ((BigDecimal)dropOffItems.stream().filter(Objects::nonNull).map(DropOffItem::getAmountCollected).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
