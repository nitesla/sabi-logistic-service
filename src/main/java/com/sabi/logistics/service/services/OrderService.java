package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.AuditTrailService;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.AuditTrailFlag;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.framework.utils.Utility;
import com.sabi.logistics.core.dto.request.OrderOrderItemDto;
import com.sabi.logistics.core.dto.request.OrderRequestDto;
import com.sabi.logistics.core.dto.response.OrderItemResponseDto;
import com.sabi.logistics.core.dto.response.OrderOrderItemResponseDto;
import com.sabi.logistics.core.dto.response.OrderResponseDto;
import com.sabi.logistics.core.models.Order;
import com.sabi.logistics.core.models.OrderItem;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.OrderItemRepository;
import com.sabi.logistics.service.repositories.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final ModelMapper mapper;
    private final AuditTrailService auditTrailService;
    @Autowired
    private Validations validations;

    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private OrderItemService orderItemService;


    public OrderService(OrderRepository orderRepository, ModelMapper mapper,AuditTrailService auditTrailService) {
        this.orderRepository = orderRepository;
        this.mapper = mapper;
        this.auditTrailService = auditTrailService;
    }

    public OrderResponseDto createOrder(OrderRequestDto request,HttpServletRequest request1) {
        validations.validateOrder(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Order order = mapper.map(request,Order.class);

        order.setReferenceNo(validations.generateReferenceNumber(10));
        Order orderExists = orderRepository.findByReferenceNo(order.getReferenceNo());
        if(order.getReferenceNo() == null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Order does not have Reference Number");
        }

        if(orderExists != null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Order already exist");
        }

        order.setBarCode(validations.generateCode(order.getReferenceNo()));
        order.setQrCode(validations.generateCode(order.getReferenceNo()));

        order.setCreatedBy(userCurrent.getId());
        order.setIsActive(true);
        order = orderRepository.save(order);
        log.debug("Create new order - {}"+ new Gson().toJson(order));

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Create new order by :" + userCurrent.getUsername(),
                        AuditTrailFlag.CREATE,
                        " Create new order for:" + order.getCustomerName() + " "+ order.getReferenceNo() ,1, Utility.getClientIp(request1));
        OrderResponseDto orderResponseDto = mapper.map(order, OrderResponseDto.class);
        return orderResponseDto;
    }

    public OrderOrderItemResponseDto createOrderOrderItems(OrderOrderItemDto request,HttpServletRequest request1) {
        List<OrderItemResponseDto> responseDtos = new ArrayList<>();
        validations.validateOrderOrderItems(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Order order = mapper.map(request,Order.class);
        OrderItem orderItem = mapper.map(request, OrderItem.class);

        order.setReferenceNo(validations.generateReferenceNumber(10));
        Order orderExists = orderRepository.findByReferenceNo(order.getReferenceNo());
        if(order.getReferenceNo() == null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Order does not have Reference Number");
        }
        if(orderExists != null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Order already exist");
        }

        order.setBarCode(validations.generateCode(order.getReferenceNo()));
        order.setQrCode(validations.generateCode(order.getReferenceNo()));

        order.setCreatedBy(userCurrent.getId());
        order.setIsActive(true);
        order = orderRepository.save(order);
        log.debug("Create new order - {}"+ new Gson().toJson(order));
        OrderOrderItemResponseDto orderResponseDto = mapper.map(order, OrderOrderItemResponseDto.class);
        log.info("request sent ::::::::::::::::::::::::::::::::: " + request.getOrderItemRequestDto());
        request.getOrderItemRequestDto().forEach(orderItems ->{
            orderItems.setOrderId(orderResponseDto.getId());
        });
        responseDtos = orderItemService.createOrderItems(request.getOrderItemRequestDto());
        List<OrderItemResponseDto> finalResponseDtos = responseDtos;
        responseDtos.forEach(orderItemResponseDto -> {
            orderResponseDto.setOrderItem(finalResponseDtos);
        });

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Create new order items by :" + userCurrent.getUsername(),
                        AuditTrailFlag.CREATE,
                        " Create new order items for:" + order.getCustomerName() + " "+ order.getReferenceNo() ,1, Utility.getClientIp(request1));
        return orderResponseDto;
    }

    public OrderResponseDto updateOrder(OrderRequestDto request,HttpServletRequest request1) {
        validations.validateOrder(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Order order = orderRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested order Id does not exist!"));
        mapper.map(request, order);

        order.setUpdatedBy(userCurrent.getId());
        orderRepository.save(order);
        log.debug("order record updated - {}"+ new Gson().toJson(order));

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Update order by username:" + userCurrent.getUsername(),
                        AuditTrailFlag.UPDATE,
                        " Update order Request for:" + order.getId() ,1, Utility.getClientIp(request1));
        OrderResponseDto orderResponseDto = mapper.map(order, OrderResponseDto.class);
        return orderResponseDto;
    }

    public OrderResponseDto updateOrderStatus(OrderRequestDto request) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Order order = orderRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested order Id does not exist!"));
        mapper.map(request, order);
        order.setUpdatedBy(userCurrent.getId());
        orderRepository.save(order);
        log.debug("order record updated - {}"+ new Gson().toJson(order));
        return mapper.map(order, OrderResponseDto.class);
    }

    public OrderResponseDto findOrder(Long id){
        Order order  = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested order Id does not exist!"));
        OrderResponseDto orderResponseDto = mapper.map(order, OrderResponseDto.class);
        orderResponseDto.setOrderItem(getAllOrderItems(id));

        return orderResponseDto;

    }


    public Page<Order> findAll( String referenceNo, String deliveryStatus,
                               String customerName, String customerPhone, String deliveryAddress,
                               String barCode, String qrCode, PageRequest pageRequest ){

        Page<Order> orders = orderRepository.findOrder(referenceNo, deliveryStatus, customerName, customerPhone,
                                                        deliveryAddress, barCode, qrCode, pageRequest);
        if(orders == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return orders;

    }



    public void enableDisable (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Order order  = orderRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Order Id does not exist!"));
        order.setIsActive(request.isActive());
        order.setUpdatedBy(userCurrent.getId());
        orderRepository.save(order);

    }


    public List<Order> getAll(Boolean isActive){
        List<Order> orders = orderRepository.findByIsActive(isActive);
        return orders;

    }

    public List<OrderItem> getAllOrderItems(Long orderId){
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        return orderItems;

    }
}
