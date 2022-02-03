package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.OrderItemRequestDto;
import com.sabi.logistics.core.dto.response.OrderItemResponseDto;
import com.sabi.logistics.core.models.Inventory;
import com.sabi.logistics.core.models.Order;
import com.sabi.logistics.core.models.OrderItem;
import com.sabi.logistics.core.models.Warehouse;
import com.sabi.logistics.service.helper.GenericSpecification;
import com.sabi.logistics.service.helper.SearchCriteria;
import com.sabi.logistics.service.helper.SearchOperation;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.InventoryRepository;
import com.sabi.logistics.service.repositories.OrderItemRepository;
import com.sabi.logistics.service.repositories.OrderRepository;
import com.sabi.logistics.service.repositories.WarehouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("All")
@Service
@Slf4j
public class OrderItemService {
    private final OrderItemRepository orderItemRepository;
    private final ModelMapper mapper;
    @Autowired
    private Validations validations;
    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryRepository inventoryRepository;


    public OrderItemService(OrderItemRepository orderItemRepository, ModelMapper mapper) {
        this.orderItemRepository = orderItemRepository;
        this.mapper = mapper;
    }

    public OrderItemResponseDto createOrderItem(OrderItemRequestDto request) {
        validations.validateOrderItem(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        OrderItem orderItem = mapper.map(request,OrderItem.class);
        Warehouse warehouse = warehouseRepository.getOne(request.getWareHouseId());
        orderItem.setCreatedBy(userCurrent.getId());
        orderItem.setIsActive(true);
        orderItem = orderItemRepository.save(orderItem);
        log.debug("Create new order item - {}"+ new Gson().toJson(orderItem));
        OrderItemResponseDto orderItemResponseDto = mapper.map(orderItem, OrderItemResponseDto.class);
        orderItemResponseDto.setWareHouseName(warehouse.getName());
        return orderItemResponseDto;
    }

    public  List<OrderItemResponseDto> createOrderItems(List<OrderItemRequestDto> requests) {
        List<OrderItemResponseDto> responseDtos = new ArrayList<>();
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        requests.forEach(request->{
            validations.validateOrderItem(request);
            OrderItem orderItem = mapper.map(request,OrderItem.class);
            orderItem.setCreatedBy(userCurrent.getId());
            orderItem.setIsActive(true);
            orderItem = orderItemRepository.save(orderItem);
            log.debug("Create new asset picture - {}"+ new Gson().toJson(orderItem));
            responseDtos.add(mapper.map(orderItem, OrderItemResponseDto.class));
        });
        return responseDtos;
    }

    public OrderItemResponseDto updateOrderItem(OrderItemRequestDto request) {
        validations.validateOrderItem(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        OrderItem orderItem = orderItemRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested order item Id does not exist!"));
        mapper.map(request, orderItem);

        orderItem.setUpdatedBy(userCurrent.getId());
        orderItemRepository.save(orderItem);
        log.debug("record updated - {}"+ new Gson().toJson(orderItem));
        OrderItemResponseDto orderItemResponseDto = mapper.map(orderItem, OrderItemResponseDto.class);
        if(request.getWareHouseId() != null ) {
            Warehouse warehouse = warehouseRepository.getOne(request.getWareHouseId());
            orderItemResponseDto.setWareHouseName(warehouse.getName());
        }

        return orderItemResponseDto;
    }

    public OrderItemResponseDto findOrderItem(Long id){
        OrderItem orderItem  = orderItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested order item Id does not exist!"));

        OrderItemResponseDto orderItemResponseDto = mapper.map(orderItem, OrderItemResponseDto.class);

        Order order = orderRepository.getOne(orderItem.getOrderId());
        orderItemResponseDto.setCustomerName(order.getCustomerName());
        orderItemResponseDto.setDeliveryAddress(order.getDeliveryAddress());
        orderItemResponseDto.setUnitPrice(orderItem.getUnitPrice());
        orderItemResponseDto.setTotalPrice(orderItem.getTotalPrice());
        orderItemResponseDto.setReferenceNo(order.getReferenceNo());

        if(orderItem.getInventoryId() != null) {
            Inventory inventory = inventoryRepository.getOne(orderItem.getInventoryId());
            orderItemResponseDto.setCreatedDate(inventory.getCreatedDate());
            orderItemResponseDto.setAcceptedDate(inventory.getAcceptedDate());
        }

        return orderItemResponseDto;

    }


    public Page<OrderItem> findAll(Long wareHouseId, String deliveryStatus, Boolean hasInventory,
                                   String productName, Integer qty,  PageRequest pageRequest ){

        GenericSpecification<OrderItem> genericSpecification = new GenericSpecification<OrderItem>();

        if (wareHouseId != null)
        {
            genericSpecification.add(new SearchCriteria("wareHouseId", wareHouseId, SearchOperation.EQUAL));
        }


        if (deliveryStatus != null && !deliveryStatus.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("deliveryStatus", deliveryStatus, SearchOperation.EQUAL));
        }

        if (hasInventory != null) {
            genericSpecification.add(new SearchCriteria("inventoryId", 0, SearchOperation.GREATER_THAN));
        }

        if (productName != null && !productName.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("productName", productName, SearchOperation.MATCH));
        }
        if (qty != null)
        genericSpecification.add(new SearchCriteria("qty", qty, SearchOperation.EQUAL));




        Page<OrderItem> orderItems = orderItemRepository.findAll(genericSpecification,pageRequest);
        if(orderItems == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }

        orderItems.getContent().forEach(item ->{
            Order order = orderRepository.getOne(item.getOrderId());
            item.setCustomerName(order.getCustomerName());
            item.setDeliveryAddress(order.getDeliveryAddress());

            if(item.getInventoryId() != null) {
                Inventory inventory = inventoryRepository.getOne(item.getInventoryId());
                item.setCreatedDate(inventory.getCreatedDate());
                item.setAcceptedDate(inventory.getAcceptedDate());
            }

        });


        return orderItems;

    }



    public void enableDisable (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        OrderItem orderItem  = orderItemRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Order item Id does not exist!"));
        orderItem.setIsActive(request.isActive());
        orderItem.setUpdatedBy(userCurrent.getId());
        orderItemRepository.save(orderItem);

    }


    public List<OrderItem> getAll(Boolean isActive){
        return orderItemRepository.findByIsActive(isActive);

    }

    public Page<OrderItem> getAllDeliveries(Long partnerId, String deliveryStatus, PageRequest pageRequest ){
        GenericSpecification<OrderItem> genericSpecification = new GenericSpecification<OrderItem>();
        if (partnerId != null) {
            Warehouse warehouse = warehouseRepository.findByPartnerId(partnerId);

            if (warehouse.getId() != null) {
                genericSpecification.add(new SearchCriteria("wareHouseId", warehouse.getId(), SearchOperation.EQUAL));
            }
        }

        if (deliveryStatus != null)
        {
            genericSpecification.add(new SearchCriteria("deliveryStatus", deliveryStatus, SearchOperation.MATCH));
        }

        Page<OrderItem> orderItems = orderItemRepository.findAll(genericSpecification, pageRequest);
        if (orderItems == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No Accepted Delivery Available!");
        }

        return orderItems;

    }
}
