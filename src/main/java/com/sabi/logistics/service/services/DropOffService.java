package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.BadRequestException;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.notification.requestDto.SmsRequest;
import com.sabi.framework.notification.requestDto.WhatsAppRequest;
import com.sabi.framework.service.NotificationService;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.service.WhatsAppService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.*;
import com.sabi.logistics.core.dto.response.DropOffItemResponseDto;
import com.sabi.logistics.core.dto.response.DropOffResponseDto;
import com.sabi.logistics.core.enums.PaymentMode;
import com.sabi.logistics.core.enums.PaymentStatus;
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
import java.util.Arrays;
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
    private DriverRepository driverRepository;

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

    private final NotificationService notificationService;

    private final WhatsAppService whatsAppService;

    private final GeneralNotificationService generalNotificationService;


    public DropOffService(DropOffRepository dropOffRepository, ModelMapper mapper, NotificationService notificationService, WhatsAppService whatsAppService, GeneralNotificationService generalNotificationService) {
        this.dropOffRepository = dropOffRepository;
        this.mapper = mapper;
        this.notificationService = notificationService;
        this.whatsAppService = whatsAppService;
        this.generalNotificationService = generalNotificationService;
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

        if (order.getPaymentStatus() == PaymentStatus.paid){
            dropOff.setPaymentMode(PaymentMode.ONLINE);
        }

        dropOff = dropOffRepository.save(dropOff);
        log.debug("Create new trip item - {}"+ new Gson().toJson(dropOff));
        DropOffResponseDto dropOffResponseDto = mapper.map(dropOff, DropOffResponseDto.class);
        dropOffResponseDto.setDeliveryAddress(order.getDeliveryAddress());

        //send notifications of the deliveryCode
        String message = "This is your Sabi DroppOff Delivery Code "+dropOff.getDeliveryCode();
        generalNotificationService.dispatchNotificationsToUser(null,order.getCustomerPhone(),message);


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

            if (order.getPaymentStatus() == PaymentStatus.paid){
                dropOff.setPaymentMode(PaymentMode.ONLINE);
            }

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
            //send notifications of the deliveryCode
            SmsRequest smsRequest = SmsRequest.builder().build();
            WhatsAppRequest whatsAppRequest = WhatsAppRequest.builder().build();
            smsRequest.setPhoneNumber(order.getCustomerPhone());
            smsRequest.setMessage("This is your Sabi DroppOff Delivery Code "+dropOff.getDeliveryCode());
            whatsAppRequest.setMessage("This is your Sabi DroppOff Delivery Code "+dropOff.getDeliveryCode());
            whatsAppRequest.setPhoneNumber(order.getCustomerPhone());
            notificationService.smsNotificationRequest(smsRequest);
            whatsAppService.whatsAppNotification(whatsAppRequest);
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

        if (dropOff.getPaymentStatus() != null) {
            if (dropOff.getPaymentStatus() == PaymentStatus.paid) {
                dropOff.setPaidStatus("paid");
            }
            if (dropOff.getPaymentStatus() == PaymentStatus.PayOnDelivery) {
                dropOff.setPaidStatus("pending");
            }
        }

        if (dropOff.getPaymentStatus() == PaymentStatus.paid){
            dropOff.setPaymentMode(PaymentMode.ONLINE);

            List<DropOffItem> dropOffItems = dropOffItemRepository.findByDropOffId(dropOff.getId());
            for (DropOffItem dropOffItem : dropOffItems) {
                OrderItem orderItem = orderItemRepository.getOne(dropOffItem.getOrderItemId());
                dropOffItem.setTransactionReference(orderItem.getPaymentReference());
                dropOffItemRepository.save(dropOffItem);
            }
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


        if (!request.getDeliveryStatus().equalsIgnoreCase("failed") && request.getDeliveryCode() == null) {
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Delivery Code cannot be empty");
        }
        if (request.getDeliveryCode() != null) {
            if (!request.getDeliveryCode().equalsIgnoreCase(dropOff.getDeliveryCode())){
                throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Invalid Delivery Code");
            }
        }

        if (request.getDeliveryStatus().equalsIgnoreCase("completed") && dropOff.getPaymentStatus() == PaymentStatus.PayOnDelivery && (!(request.getTotalAmount().equals(dropOff.getTotalAmount())))) {
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Invalid Amount");
        }

        if (request.getDeliveryStatus().equalsIgnoreCase("PartiallyCompleted") && dropOff.getPaymentStatus() == PaymentStatus.PayOnDelivery  && request.getTotalAmount().equals(0)) {
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

        if (dropOff.getPaymentStatus() != null) {
            if (dropOff.getPaymentStatus() == PaymentStatus.paid) {
                dropOff.setPaidStatus("paid");
            }
            if (dropOff.getPaymentStatus() == PaymentStatus.PayOnDelivery ) {
                dropOff.setPaidStatus("pending");
            }
        }

        if (dropOff.getPaymentStatus() == PaymentStatus.paid){
            dropOff.setPaymentMode(PaymentMode.ONLINE);

            List<DropOffItem> dropOffItems = dropOffItemRepository.findByDropOffId(dropOff.getId());
            for (DropOffItem dropOffItem : dropOffItems) {
                OrderItem orderItem = orderItemRepository.getOne(dropOffItem.getOrderItemId());
                dropOffItem.setTransactionReference(orderItem.getPaymentReference());
                dropOffItemRepository.save(dropOffItem);
            }
        }

        dropOff.setUpdatedBy(userCurrent.getId());
        dropOffRepository.save(dropOff);

        DropOffResponseDto dropOffResponseDto = mapper.map(dropOff, DropOffResponseDto.class);


        TripRequest tripRequest = tripRequestRepository.getOne(dropOff.getTripRequestId());

        if(dropOff.getFinalDropOff() == true) {
            tripRequest.setStatus("completed");

            List<DropOff> dropItems = dropOffRepository.findByTripRequestId(dropOff.getTripRequestId());
//                if(dropItems.stream().map(DropOffItem::getStatus).allMatch(response -> dropOffItem.getStatus().equals("completed"))){
            if (dropItems.stream().allMatch(response -> response.getDeliveryStatus().equalsIgnoreCase("completed"))) {
                tripRequest.setDeliveryStatus("completed");
            } else if (dropItems.stream().allMatch(response -> response.getDeliveryStatus().equalsIgnoreCase("failed"))) {
                tripRequest.setDeliveryStatus("failed");
            } else if (dropItems.stream().anyMatch(response -> response.getDeliveryStatus().equalsIgnoreCase("completed")) && dropItems.stream().anyMatch(response -> response.getDeliveryStatus().equalsIgnoreCase("failed"))) {
                tripRequest.setDeliveryStatus("PartiallyCompleted");
            } else if (dropItems.stream().anyMatch(response -> response.getDeliveryStatus().equalsIgnoreCase("PartiallyCompleted"))) {
                tripRequest.setDeliveryStatus("PartiallyCompleted");
            }
        }

        tripRequestRepository.save(tripRequest);

        if(request.getDropOffItem() != null) {
            List<DropOffItemResponseDto> dropOffItems = dropOffItemService.updateDropOffItemStatus(request.getDropOffItem(), dropOffResponseDto.getId());
            dropOff.setDropOffItem(dropOffItemRepository.findByDropOffId(dropOff.getId()));
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

        if (dropOff.getPaymentStatus() != null && dropOffResponseDto.getPaymentStatus() == PaymentStatus.PayOnDelivery ) {
            List<DropOffItem> dropOffItems = dropOffItemRepository.findByDropOffId(id);
            dropOffResponseDto.setTotalAmount(getTotalAmount(dropOffItems));
            dropOffResponseDto.setAmountCollected(getTotalAmountCollected(dropOffItems));
        }
        return dropOffResponseDto;
    }


    public Page<DropOff> findAll(Long orderId, Long tripRequestId, PageRequest pageRequest ){

        Page<DropOff> dropOffs = dropOffRepository.findDropOff(orderId, tripRequestId,pageRequest);
        if(dropOffs == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        dropOffs.getContent().stream().forEach((dropOff)->dropOff.setDropOffItem(getAllDropOffItems(dropOff.getId())));
        dropOffs.getContent().stream().filter((dropOff)->dropOff.getPaymentStatus()!=null && dropOff.getPaymentStatus() == PaymentStatus.PayOnDelivery ).forEach((dropOff)->dropOff.setTotalAmount(getTotalAmount(dropOff.getDropOffItem())));
        dropOffs.getContent().stream().filter((dropOff)->dropOff.getPaymentStatus()!=null && dropOff.getPaymentStatus() == PaymentStatus.PayOnDelivery ).forEach((dropOff)->dropOff.setAmountCollected(getTotalAmountCollected(dropOff.getDropOffItem())));


        return dropOffs;

    }
    private List<DropOff> setAndCaluateDroppOffsParameters(List<DropOff> dropOffs){
        dropOffs.stream().forEach((dropOff)->dropOff.setDropOffItem(getAllDropOffItems(dropOff.getId())));
        dropOffs.stream().filter((dropOff)->dropOff.getPaymentStatus()!=null && dropOff.getPaymentStatus() == PaymentStatus.PayOnDelivery ).forEach((dropOff)->dropOff.setTotalAmount(getTotalAmount(dropOff.getDropOffItem())));
        dropOffs.stream().filter((dropOff)->dropOff.getPaymentStatus()!=null && dropOff.getPaymentStatus() == PaymentStatus.PayOnDelivery ).forEach((dropOff)->dropOff.setAmountCollected(getTotalAmountCollected(dropOff.getDropOffItem())));
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
        List<DropOff> tripsDropOffsList = dropOffRepository.findByIsActiveAndTripRequestId(isActive, tripRequestId);
        return this.setAndCaluateDroppOffsParameters(tripsDropOffsList);

    }

    public DropOffResponseDto updatePaidStatus(DropOffUpdatePaidDto request ){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DropOff dropOff = dropOffRepository.findById(request.getDropOffId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested DropOff Id does not exist!"));

        dropOff.setPaidStatus(request.getPaidStatus());

        dropOff.setUpdatedBy(userCurrent.getId());
        dropOffRepository.save(dropOff);
        log.debug("record updated - {}"+ new Gson().toJson(dropOff));
        return mapper.map(dropOff, DropOffResponseDto.class);

    }

    public DropOffResponseDto updateReturnStatus(DropOffUpdateRequestDto dropOffUpdateRequestDto){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DropOff dropOff = dropOffRepository.findById(dropOffUpdateRequestDto.getDropOffId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested DropOff Id does not exist!"));
        Long totalQtyReturned = 0L;
        List<DropOffItem> dropOffItemList = new ArrayList<>();

        for (DropOffItemUpdateRequestDto itemDto:dropOffUpdateRequestDto.getDropOffItems()){
            DropOffItem dropOffItem = dropOffItemRepository.findById(itemDto.getDropOffItemId()).
                    orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"Requested DropoffItem id does not exist!"));
            if( dropOff.getQty()!=null && (itemDto.getQty() > dropOff.getQty())){
                throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"Quantity of items returned can't exceed total quantity of dropoff");
            }
            totalQtyReturned+=itemDto.getQty();
            dropOffItem.setQtyGoodsReturned(itemDto.getQty());
            dropOffItem.setQtyGoodsDelivered((dropOffItem.getQty()-dropOffItem.getQtyGoodsReturned()));
            dropOffItemList.add(dropOffItem);

        }
        if(dropOff.getQty()==null){
            log.info("DropOff Quantity is found to be null during dropOff update::{}",dropOff);
            throw new IllegalArgumentException("OOps, Contact the adminstrator, DropOff Quantity is null");

        }
        if (totalQtyReturned > dropOff.getQty()){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"Total number of returned quantity can't exceed total number of DropOffs");
        }
        List<DropOffItem> dropOffItemUpdateResult = dropOffItemRepository.saveAll(dropOffItemList);
        dropOff.setQtyReturned(totalQtyReturned.intValue());
        dropOff.setReturnStatus(dropOffUpdateRequestDto.getReturnedStatus());
        dropOff.setQtyDelivered(Math.toIntExact((Long.valueOf(dropOff.getQty() - dropOff.getQtyReturned()))));
        dropOff.setUpdatedBy(userCurrent.getId());

        dropOffRepository.save(dropOff);
        dropOff.setDropOffItem(dropOffItemUpdateResult);
        log.debug("record updated - {}"+ new Gson().toJson(dropOff));
        return mapper.map(dropOff, DropOffResponseDto.class);

    }

    public List<DropOff> getAllDropOffs(String paidStatus, String returnedStatus,Long tripRequestId){
        List<DropOff> dropOffList = null;
        // Returns based on whether whether paidStatus is passed, returnedStatus is passed or both of them are passed to the method.
        if (paidStatus!=null && returnedStatus == null){
            dropOffList = dropOffRepository.findByTripRequestIdAndPaidStatus(tripRequestId,paidStatus);
        }
        else if (returnedStatus != null && paidStatus == null){
            dropOffList = dropOffRepository.findByTripRequestIdAndReturnStatus(tripRequestId,returnedStatus);
        }
        else {
            dropOffList = dropOffRepository.findByTripRequestIdAndPaidStatusAndReturnStatus(tripRequestId,paidStatus,returnedStatus);
        }
        for (DropOff dropOff : dropOffList) {

            Order order = orderRepository.getOne(dropOff.getOrderId());
            dropOff.setCustomerName(order.getCustomerName());
            dropOff.setDeliveryAddress(order.getDeliveryAddress());
            dropOff.setCustomerPhone(order.getCustomerPhone());


            if (dropOff.getPaymentStatus() != null && dropOff.getPaymentStatus() == PaymentStatus.PayOnDelivery ) {
                List<DropOffItem> dropOffItems = dropOffItemRepository.findByDropOffId(dropOff.getId());
                dropOff.setTotalAmount(getTotalAmount(dropOffItems));
                dropOff.setAmountCollected(getTotalAmountCollected(dropOffItems));
            }

            dropOff.setDropOffItem(getAllDropOffItems(dropOff.getId()));
        }



        return dropOffList;

    }

    public List<DropOff> getAlLDropOffsOfADriver(Long driverUserId, String returnedStatus){
        Driver driver = driverRepository.findByUserId(driverUserId);
        if (driver == null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"Driver doesn't exist");
        }
        List<TripRequest> tripRequestList = tripRequestRepository.findByDriverId(driver.getId());
        if(tripRequestList.size() == 0){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"This driver doesn't have any trips at the moment");
        }
        if(!Arrays.asList("none","returned","pending").stream().anyMatch(s -> s.equalsIgnoreCase(returnedStatus))){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"The given returnedStatus does not exist");
        }
        List<DropOff> dropOffList = dropOffRepository.getAllDropOffsOfADriver(driverUserId,returnedStatus);
        log.info("The Size of Retrived Droppoffs =={}",dropOffList.size());

        return setAndCaluateDroppOffsParameters(dropOffList);
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
        return ((BigDecimal)dropOffItems.stream().filter(Objects::nonNull).map(DropOffItem::getTotalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
    private BigDecimal getTotalAmountCollected(List<DropOffItem> dropOffItems) {
        return ((BigDecimal)dropOffItems.stream().filter(Objects::nonNull).map(DropOffItem::getAmountCollected).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
