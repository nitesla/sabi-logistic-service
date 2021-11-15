package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.OrderRequestDto;
import com.sabi.logistics.core.dto.response.OrderResponseDto;
import com.sabi.logistics.core.models.Order;
import com.sabi.logistics.core.models.OrderItem;
import com.sabi.logistics.core.models.Warehouse;
import com.sabi.logistics.service.helper.GenericSpecification;
import com.sabi.logistics.service.helper.SearchCriteria;
import com.sabi.logistics.service.helper.SearchOperation;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.OrderItemRepository;
import com.sabi.logistics.service.repositories.OrderRepository;
import com.sabi.logistics.service.repositories.WarehouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final ModelMapper mapper;
    @Autowired
    private Validations validations;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;


    public OrderService(OrderRepository orderRepository, ModelMapper mapper) {
        this.orderRepository = orderRepository;
        this.mapper = mapper;
    }

    public OrderResponseDto createOrder(OrderRequestDto request) {
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
        Warehouse warehouse = warehouseRepository.getOne(request.getWareHouseID());

        order.setBarCode(validations.generateCode(order.getReferenceNo()));
        order.setQrCode(validations.generateCode(order.getReferenceNo()));

        order.setCreatedBy(userCurrent.getId());
        order.setIsActive(true);
        order = orderRepository.save(order);
        log.debug("Create new order - {}"+ new Gson().toJson(order));
        OrderResponseDto orderResponseDto = mapper.map(order, OrderResponseDto.class);
        orderResponseDto.setWareHouseName(warehouse.getName());
        return orderResponseDto;
    }

    public OrderResponseDto updateOrder(OrderRequestDto request) {
        validations.validateOrder(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Order order = orderRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested order Id does not exist!"));
        mapper.map(request, order);

        order.setUpdatedBy(userCurrent.getId());
        orderRepository.save(order);
        log.debug("order record updated - {}"+ new Gson().toJson(order));
        OrderResponseDto orderResponseDto = mapper.map(order, OrderResponseDto.class);
        if(request.getWareHouseID() != null ) {
            Warehouse warehouse = warehouseRepository.getOne(request.getWareHouseID());
            orderResponseDto.setWareHouseName(warehouse.getName());
        }

        return orderResponseDto;
    }

    public OrderResponseDto findOrder(Long id){
        Order order  = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested order Id does not exist!"));
        OrderResponseDto orderResponseDto = mapper.map(order, OrderResponseDto.class);
        orderResponseDto.setOrderItem(getAllOrderItems(id));

        return orderResponseDto;

    }


    public Page<Order> findAll(Long wareHouseID, String referenceNo, String deliveryStatus,
                               String customerName, String customerPhone, String deliveryAddress,
                               String barCode, String qrCode, PageRequest pageRequest ){
        GenericSpecification<Order> genericSpecification = new GenericSpecification<Order>();

        if (wareHouseID != null)
        {
            genericSpecification.add(new SearchCriteria("wareHouseID", wareHouseID, SearchOperation.EQUAL));
        }

        if (referenceNo != null && !referenceNo.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("referenceNo", referenceNo, SearchOperation.EQUAL));
        }

        if (deliveryStatus != null && !deliveryStatus.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("deliveryStatus", deliveryStatus, SearchOperation.MATCH));
        }

        if (customerName != null && !customerName.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("customerName", customerName, SearchOperation.MATCH));
        }

        if (customerPhone != null && !customerPhone.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("customerPhone", deliveryStatus, SearchOperation.MATCH));
        }

        if (deliveryAddress != null && !deliveryAddress.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("deliveryAddress", deliveryAddress, SearchOperation.MATCH));
        }

        if (barCode != null && !barCode.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("barCode", barCode, SearchOperation.MATCH));
        }

        if (qrCode != null && !qrCode.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("QRcode", qrCode, SearchOperation.MATCH));
        }

        Page<Order> orders = orderRepository.findAll(genericSpecification, pageRequest);
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

    public List<OrderItem> getAllOrderItems(Long orderID){
        List<OrderItem> orderItems = orderItemRepository.findByOrderID(orderID);
        return orderItems;

    }
}
