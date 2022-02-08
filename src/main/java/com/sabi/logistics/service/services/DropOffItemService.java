package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.BadRequestException;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.service.AuditTrailService;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.AuditTrailFlag;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.framework.utils.Utility;
import com.sabi.logistics.core.dto.request.DropOffItemRequestDto;
import com.sabi.logistics.core.dto.request.TripItemRequestDto;
import com.sabi.logistics.core.dto.response.DropOffItemResponseDto;
import com.sabi.logistics.core.models.*;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@SuppressWarnings("All")
@Service
@Slf4j
public class DropOffItemService {
    private final DropOffItemRepository dropOffItemRepository;
    private final ModelMapper mapper;
    @Autowired
    private Validations validations;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TripRequestRepository tripRequestRepository;

    @Autowired
    private TripItemRepository tripItemRepository;

    @Autowired
    private DropOffRepository dropOffRepository;

    @Autowired
    private TripItemService tripItemService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private UserRepository userRepository;


    public DropOffItemService(DropOffItemRepository dropOffItemRepository, ModelMapper mapper) {
        this.dropOffItemRepository = dropOffItemRepository;
        this.mapper = mapper;
    }

    public DropOffItemResponseDto createDropOffItem(DropOffItemRequestDto request,HttpServletRequest request1) {
        validations.validateDropOffItem(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DropOffItem dropOffItem = mapper.map(request,DropOffItem.class);
        DropOffItem dropOffItemExists = dropOffItemRepository.findByOrderItemIdAndDropOffId(request.getOrderItemId(), request.getDropOffId());
        if(dropOffItemExists !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " DropOff Item already exist");
        }
        if (!(request.getStatus().equalsIgnoreCase("completed") || request.getStatus().equalsIgnoreCase("cancelled"))) {
            DropOffItem dropOffItemm = dropOffItemRepository.findByOrderItemIdAndStatus(request.getOrderItemId(), request.getStatus());
            if (dropOffItemm != null) {
                throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " DropOff Item already exist");
            }
        }
        OrderItem orderItem = orderItemRepository.getOne(request.getOrderItemId());
        Order order = orderRepository.getOne(orderItem.getOrderId());
        DropOff dropOff = dropOffRepository.getOne(request.getDropOffId());
        dropOffItem.setCreatedBy(userCurrent.getId());
        dropOffItem.setIsActive(true);
        dropOffItem = dropOffItemRepository.save(dropOffItem);
        log.debug("Create new trip item - {}"+ new Gson().toJson(dropOffItem));
        DropOffItemResponseDto dropOffItemResponseDto = mapper.map(dropOffItem, DropOffItemResponseDto.class);
        dropOffItemResponseDto.setOrderItemName(orderItem.getProductName());
        dropOffItemResponseDto.setThirdPartyProductId(orderItem.getThirdPartyProductId());
        dropOffItemResponseDto.setQty(orderItem.getQty());
        dropOffItemResponseDto.setCustomerName(order.getCustomerName());
        dropOffItemResponseDto.setCustomerPhone(order.getCustomerPhone());
        dropOffItemResponseDto.setOrderId(orderItem.getOrderId());
        dropOffItemResponseDto.setDropOffId(dropOffItem.getDropOffId());
        dropOffItemResponseDto.setOrderItemId(dropOffItem.getOrderItemId());

        List<DropOffItem> dropOffItems = new ArrayList<>();
        List<OrderItem> orderItems = new ArrayList<>();
        TripItem tripItem = new TripItem();
        TripItemRequestDto tripItemRequestDto = new TripItemRequestDto();

        tripItem = tripItemRepository.findByTripRequestIdAndThirdPartyProductId(dropOff.getTripRequestId(), orderItem.getThirdPartyProductId());
        dropOffItems = dropOffItemRepository.findByTripRequestIdAndThirdPartyProductId(dropOff.getTripRequestId(), orderItem.getThirdPartyProductId());
        orderItems = orderItemRepository.findByOrderIdAndThirdPartyProductId(dropOff.getOrderId(), orderItem.getThirdPartyProductId());

        if(tripItem == null) {
            tripItemRequestDto.setTripRequestId(dropOff.getTripRequestId());
            tripItemRequestDto.setThirdPartyProductId(orderItem.getThirdPartyProductId());
            tripItemRequestDto.setProductName(orderItem.getProductName());
            tripItemRequestDto.setQty(getQty(orderItems));
            tripItemRequestDto.setQtyPickedUp(getQtyPickedUp(dropOffItems));
            tripItemService.createTripItem(tripItemRequestDto);
        } else {
            tripItemRequestDto.setTripRequestId(dropOff.getTripRequestId());
            tripItemRequestDto.setThirdPartyProductId(orderItem.getThirdPartyProductId());
            tripItemRequestDto.setProductName(orderItem.getProductName());
            tripItemRequestDto.setQty(getQty(orderItems));
            tripItemRequestDto.setQtyPickedUp(getQtyPickedUp(dropOffItems));
            tripItemRequestDto.setId(tripItem.getId());
            tripItemService.updateTripItem(tripItemRequestDto);
        }
        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Create new dropOffItem  by :" + userCurrent.getUsername(),
                        AuditTrailFlag.CREATE,
                        " Create new dropOffItem for:" + dropOffItem.getCustomerName() ,1, Utility.getClientIp(request1));

        return dropOffItemResponseDto;
    }

    public List<DropOffItem> createDropOffItems(List<DropOffItemRequestDto> requests, Long dropOffId) {
        List<DropOffItem> responseDtos = new ArrayList<>();
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        requests.forEach(request-> {
            request.setDropOffId(dropOffId);
            validations.validateDropOffItem(request);
            DropOffItem dropOffItemExists = dropOffItemRepository.findByOrderItemIdAndDropOffId(request.getOrderItemId(), request.getDropOffId());
            if(dropOffItemExists !=null){
                throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " DropOff Item already exist");
            }

            if (!(request.getStatus().equalsIgnoreCase("completed") || request.getStatus().equalsIgnoreCase("cancelled"))) {
                DropOffItem dropOffItemm = dropOffItemRepository.findByOrderItemIdAndStatus(request.getOrderItemId(), request.getStatus());
                if (dropOffItemm != null) {
                    throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " DropOff Item already exist");
                }
            }

            DropOffItem dropOffItem = mapper.map(request, DropOffItem.class);
            OrderItem orderItem = orderItemRepository.getOne(request.getOrderItemId());
            Order order = orderRepository.getOne(orderItem.getOrderId());
            DropOff dropOff = dropOffRepository.getOne(request.getDropOffId());
            dropOffItem.setCreatedBy(userCurrent.getId());
            dropOffItem.setIsActive(true);
            dropOffItem = dropOffItemRepository.save(dropOffItem);
            log.debug("Create new trip item - {}" + new Gson().toJson(dropOffItem));
            DropOffItem dropOffItemResponseDto = mapper.map(dropOffItem, DropOffItem.class);
            dropOffItemResponseDto.setOrderItemName(orderItem.getProductName());
            dropOffItemResponseDto.setThirdPartyProductId(orderItem.getThirdPartyProductId());
            dropOffItemResponseDto.setQty(orderItem.getQty());
            dropOffItemResponseDto.setCustomerName(order.getCustomerName());
            dropOffItemResponseDto.setCustomerPhone(order.getCustomerPhone());
            dropOffItemResponseDto.setOrderId(orderItem.getOrderId());
            dropOffItemResponseDto.setDropOffId(dropOffItem.getDropOffId());
            dropOffItemResponseDto.setOrderItemId(dropOffItem.getOrderItemId());

            orderItem.setDeliveryStatus("AwaitingDelivery");
            orderItemRepository.save(orderItem);

            List<DropOffItem> dropOffItems = new ArrayList<>();
            List<OrderItem> orderItems = new ArrayList<>();
            TripItem tripItem = new TripItem();
            TripItemRequestDto tripItemRequestDto = new TripItemRequestDto();

            tripItem = tripItemRepository.findByTripRequestIdAndThirdPartyProductId(dropOff.getTripRequestId(), orderItem.getThirdPartyProductId());
            dropOffItems = dropOffItemRepository.findByTripRequestIdAndThirdPartyProductId(dropOff.getTripRequestId(), orderItem.getThirdPartyProductId());
            orderItems = orderItemRepository.findByOrderIdAndThirdPartyProductId(dropOff.getOrderId(), orderItem.getThirdPartyProductId());

            if (tripItem == null) {
                tripItemRequestDto.setTripRequestId(dropOff.getTripRequestId());
                tripItemRequestDto.setThirdPartyProductId(orderItem.getThirdPartyProductId());
                tripItemRequestDto.setProductName(orderItem.getProductName());
                tripItemRequestDto.setQty(getQty(orderItems));
                tripItemRequestDto.setQtyPickedUp(getQtyPickedUp(dropOffItems));
                tripItemService.createTripItem(tripItemRequestDto);
            } else {
                tripItemRequestDto.setTripRequestId(dropOff.getTripRequestId());
                tripItemRequestDto.setThirdPartyProductId(orderItem.getThirdPartyProductId());
                tripItemRequestDto.setProductName(orderItem.getProductName());
                tripItemRequestDto.setQty(getQty(orderItems));
                tripItemRequestDto.setQtyPickedUp(getQtyPickedUp(dropOffItems));
                tripItemRequestDto.setId(tripItem.getId());
                tripItemService.updateTripItem(tripItemRequestDto);
            }



            responseDtos.add(dropOffItemResponseDto);
        });

        return responseDtos;
    }

    public DropOffItemResponseDto updateDropOffItem(DropOffItemRequestDto request,HttpServletRequest request1) {
        validations.validateDropOffItem(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DropOffItem dropOffItem = dropOffItemRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested dropOffItem Id does not exist!"));
        OrderItem orderItem = orderItemRepository.getOne(request.getOrderItemId());
        Order order = orderRepository.getOne(orderItem.getOrderId());
        DropOff dropOff = dropOffRepository.getOne(request.getDropOffId());
        mapper.map(request, dropOffItem);
        dropOffItem.setUpdatedBy(userCurrent.getId());
        if(request.getQtyGoodsDelivered() != null && request.getQtyGoodsDelivered() > orderItem.getQty()){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Quantity of Items Delivered can't be greater than Total Quantity");
        }
        dropOffItemRepository.save(dropOffItem);
        log.debug("record updated - {}"+ new Gson().toJson(dropOffItem));
        DropOffItemResponseDto dropOffItemResponseDto = mapper.map(dropOffItem, DropOffItemResponseDto.class);

        if(request.getOrderItemId() != null ) {

            dropOffItemResponseDto.setOrderItemName(orderItem.getProductName());
            dropOffItemResponseDto.setQty(orderItem.getQty());
            dropOffItemResponseDto.setCustomerName(order.getCustomerName());
            dropOffItemResponseDto.setCustomerPhone(order.getCustomerPhone());
            dropOffItemResponseDto.setOrderId(orderItem.getOrderId());
            dropOffItemResponseDto.setDropOffId(dropOffItem.getDropOffId());
            dropOffItemResponseDto.setOrderItemId(dropOffItem.getOrderItemId());
        }

        List<DropOffItem> dropOffItems = new ArrayList<>();
        List<OrderItem> orderItems = new ArrayList<>();
        TripItem tripItem = new TripItem();
        TripItemRequestDto tripItemRequestDto = new TripItemRequestDto();

        tripItem = tripItemRepository.findByTripRequestIdAndThirdPartyProductId(dropOff.getTripRequestId(), orderItem.getThirdPartyProductId());
        dropOffItems = dropOffItemRepository.findByTripRequestIdAndThirdPartyProductId(dropOff.getTripRequestId(), orderItem.getThirdPartyProductId());
        orderItems = orderItemRepository.findByOrderIdAndThirdPartyProductId(dropOff.getOrderId(), orderItem.getThirdPartyProductId());

        if(tripItem == null) {
            tripItemRequestDto.setTripRequestId(dropOff.getTripRequestId());
            tripItemRequestDto.setThirdPartyProductId(orderItem.getThirdPartyProductId());
            tripItemRequestDto.setProductName(orderItem.getProductName());
            tripItemRequestDto.setQty(getQty(orderItems));
            tripItemRequestDto.setQtyPickedUp(getQtyPickedUp(dropOffItems));
            tripItemService.createTripItem(tripItemRequestDto);
        } else {
            tripItemRequestDto.setTripRequestId(dropOff.getTripRequestId());
            tripItemRequestDto.setThirdPartyProductId(orderItem.getThirdPartyProductId());
            tripItemRequestDto.setProductName(orderItem.getProductName());
            tripItemRequestDto.setQty(getQty(orderItems));
            tripItemRequestDto.setQtyPickedUp(getQtyPickedUp(dropOffItems));
            tripItemRequestDto.setId(tripItem.getId());
            tripItemService.updateTripItem(tripItemRequestDto);
        }


        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Update dropOffItem by username:" + userCurrent.getUsername(),
                        AuditTrailFlag.UPDATE,
                        " Update dropOffItem Request for:" + dropOffItem.getId(),1, Utility.getClientIp(request1));
        return dropOffItemResponseDto;
    }

    public List<DropOffItemResponseDto> updateDropOffItemStatus(List<DropOffItemRequestDto> requests, Long dropOffId) {

        List<DropOffItemResponseDto> responseDtos = new ArrayList<>();
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        requests.forEach(request-> {
            DropOffItem dropOffItem = dropOffItemRepository.findById(request.getId())
                    .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                            "Requested dropOffItem Id does not exist!"));
            request.setDropOffId(dropOffId);
            validations.validateDropOffItem(request);
            OrderItem orderItem = orderItemRepository.getOne(dropOffItem.getOrderItemId());

            if(request.getQtyGoodsDelivered() != null && request.getQtyGoodsDelivered() > orderItem.getQty()){
                throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Quantity of Items Delivered can't be greater than Total Quantity");
            }

            mapper.map(request, dropOffItem);
            dropOffItem.setUpdatedBy(userCurrent.getId());
            dropOffItemRepository.save(dropOffItem);
            log.debug("record updated - {}"+ new Gson().toJson(dropOffItem));
            DropOffItemResponseDto dropOffItemResponseDto = mapper.map(dropOffItem, DropOffItemResponseDto.class);
            responseDtos.add(dropOffItemResponseDto);
        });

        return responseDtos;
    }

    public DropOffItemResponseDto findDropOffItem(Long id){
        DropOffItem dropOffItem  = dropOffItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested trip item Id does not exist!"));
        DropOffItemResponseDto dropOffItemResponseDto = mapper.map(dropOffItem, DropOffItemResponseDto.class);
        OrderItem orderItem = orderItemRepository.getOne(dropOffItem.getOrderItemId());

        Order order = orderRepository.getOne(orderItem.getOrderId());

        dropOffItemResponseDto.setOrderItemName(orderItem.getProductName());
        dropOffItemResponseDto.setQty(orderItem.getQty());
        dropOffItemResponseDto.setCustomerName(order.getCustomerName());
        dropOffItemResponseDto.setCustomerPhone(order.getCustomerPhone());
        dropOffItemResponseDto.setOrderId(orderItem.getOrderId());
        dropOffItemResponseDto.setDropOffId(dropOffItem.getDropOffId());
        dropOffItemResponseDto.setOrderItemId(dropOffItem.getOrderItemId());

        return dropOffItemResponseDto;
    }

    public DropOffItemResponseDto findDropOffItemDetails(Long orderItemId, String status){
        DropOffItem dropOffItem  = dropOffItemRepository.findByOrderItemIdAndStatus(orderItemId, status.toLowerCase());
        if(dropOffItem == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        DropOffItemResponseDto dropOffItemDetailDto = mapper.map(dropOffItem, DropOffItemResponseDto.class);
        OrderItem orderItem = orderItemRepository.getOne(dropOffItem.getOrderItemId());

        Order order = orderRepository.getOne(orderItem.getOrderId());

        DropOff dropOff = dropOffRepository.getOne(dropOffItem.getDropOffId());

        TripRequest tripRequest = tripRequestRepository.getOne(dropOff.getTripRequestId());

        dropOffItemDetailDto.setOrderItemName(orderItem.getProductName());
        dropOffItemDetailDto.setQty(orderItem.getQty());
        dropOffItemDetailDto.setCustomerName(order.getCustomerName());
        dropOffItemDetailDto.setCustomerPhone(order.getCustomerPhone());
        dropOffItemDetailDto.setOrderId(orderItem.getOrderId());
        dropOffItemDetailDto.setDropOffId(dropOffItem.getDropOffId());
        dropOffItemDetailDto.setOrderItemId(dropOffItem.getOrderItemId());
        if (tripRequest.getDriverId() != null) {
            Driver driver = driverRepository.findDriverById(tripRequest.getDriverId());

            User user = userRepository.getOne(driver.getUserId());
            dropOffItemDetailDto.setDriverName(user.getLastName() + " " + user.getFirstName());
            dropOffItemDetailDto.setDriverPhone(user.getPhone());
        }

        if (tripRequest.getDriverAssistantId() != null) {
            Driver driver2 = driverRepository.findDriverById(tripRequest.getDriverAssistantId());

            User user2 = userRepository.getOne(driver2.getUserId());
            dropOffItemDetailDto.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());
            dropOffItemDetailDto.setDriverAssistantPhone(user2.getPhone());
        }

        return dropOffItemDetailDto;
    }


    public Page<DropOffItem> findAll(Long orderItemId, Long dropOffId,
                                   String status, PageRequest pageRequest ){

        Page<DropOffItem> tripItems = dropOffItemRepository.findDropOffItem(orderItemId, dropOffId, status,  pageRequest);
        if(tripItems == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        tripItems.getContent().forEach(item ->{
            DropOffItem dropOffItem = dropOffItemRepository.getOne(item.getId());


            OrderItem orderItem = orderItemRepository.getOne(dropOffItem.getOrderItemId());

            Order order = orderRepository.getOne(orderItem.getOrderId());

            item.setOrderItemName(orderItem.getProductName());
            item.setQty(orderItem.getQty());
            item.setCustomerName(order.getCustomerName());
            item.setCustomerPhone(order.getCustomerPhone());
            item.setOrderId(orderItem.getOrderId());

        });



        return tripItems;

    }



    public void enableDisable (EnableDisEnableDto request,HttpServletRequest request1){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DropOffItem dropOffItem  = dropOffItemRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Trip item Id does not exist!"));
        dropOffItem.setIsActive(request.isActive());
        dropOffItem.setUpdatedBy(userCurrent.getId());

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Update dropOffItem by username:" + userCurrent.getUsername(),
                        AuditTrailFlag.UPDATE,
                        " Update dropOffItem Request for:" + dropOffItem.getId(),1, Utility.getClientIp(request1));
        dropOffItemRepository.save(dropOffItem);

    }


    public List<DropOffItem> getAll( Long dropOffId, Boolean isActive){
        List<DropOffItem> tripItems = dropOffItemRepository.findByDropOffIdAndIsActive(dropOffId, isActive);

        for (DropOffItem item : tripItems) {
            OrderItem orderItem = orderItemRepository.getOne(item.getOrderItemId());

            Order order = orderRepository.getOne(orderItem.getOrderId());

            item.setOrderItemName(orderItem.getProductName());
            item.setQty(orderItem.getQty());
            item.setCustomerName(order.getCustomerName());
            item.setCustomerPhone(order.getCustomerPhone());
            item.setOrderId(orderItem.getOrderId());

        }
        return tripItems;

    }

    public List<DropOffItem> getInvoice(Long dropOffId, Long orderId){

        List<DropOffItem> tripItems = dropOffItemRepository.findByDropOffIdAndOrderId(dropOffId, orderId);

        for (DropOffItem item : tripItems) {
            OrderItem orderItem = orderItemRepository.getOne(item.getOrderItemId());

            DropOff dropOff = dropOffRepository.findById(item.getDropOffId())
                    .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested tripRequestId does not exist!"));

            TripRequest tripRequest = tripRequestRepository.findById(dropOff.getTripRequestId())
                    .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested tripRequestId does not exist!"));

            Order order = orderRepository.getOne(orderItem.getOrderId());

            item.setOrderItemName(orderItem.getProductName());
            item.setQty(orderItem.getQty());
            item.setCustomerName(order.getCustomerName());
            item.setCustomerPhone(order.getCustomerPhone());
            item.setOrderId(orderItem.getOrderId());
            item.setDeliveryDate(tripRequest.getDateDelivered());
            item.setTax(order.getTax());
            item.setReferenceNo(tripRequest.getReferenceNo());
        }
        return tripItems;

    }

    private Integer getQty(List<OrderItem> orderItems) {
        return ((Integer)orderItems.stream().filter(Objects::nonNull).map(OrderItem::getQty).reduce(Integer.valueOf(0), Integer::sum));
    }

    private Integer getQtyPickedUp(List<DropOffItem> dropOffItems) {
        return ((Integer)dropOffItems.stream().filter(Objects::nonNull).map(DropOffItem::getQtyGoodsDelivered).reduce(Integer.valueOf(0), Integer::sum));
    }
}
