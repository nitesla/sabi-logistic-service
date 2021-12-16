package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.OrderItemRequestDto;
import com.sabi.logistics.core.dto.request.PartnerAssetPictureDto;
import com.sabi.logistics.core.dto.response.OrderItemResponseDto;
import com.sabi.logistics.core.models.OrderItem;
import com.sabi.logistics.core.models.Warehouse;
import com.sabi.logistics.service.helper.GenericSpecification;
import com.sabi.logistics.service.helper.SearchCriteria;
import com.sabi.logistics.service.helper.SearchOperation;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.OrderItemRepository;
import com.sabi.logistics.service.repositories.WarehouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OrderItemService {
    private final OrderItemRepository orderItemRepository;
    private final ModelMapper mapper;
    @Autowired
    private Validations validations;
    @Autowired
    private WarehouseRepository warehouseRepository;


    public OrderItemService(OrderItemRepository orderItemRepository, ModelMapper mapper) {
        this.orderItemRepository = orderItemRepository;
        this.mapper = mapper;
    }

    public OrderItemResponseDto createOrderItem(OrderItemRequestDto request) {
        validations.validateOrderItem(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        OrderItem orderItem = mapper.map(request,OrderItem.class);

        OrderItem orderItemExists = orderItemRepository.findByName(orderItem.getName());

        if(orderItemExists !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Order Item already exist");
        }
        Warehouse warehouse = warehouseRepository.getOne(request.getWareHouseID());
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
            OrderItem exist = orderItemRepository.findByName(request.getName());
            if(exist !=null){
                throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " order item already exist");
            }
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
        log.debug("color record updated - {}"+ new Gson().toJson(orderItem));
        OrderItemResponseDto orderItemResponseDto = mapper.map(orderItem, OrderItemResponseDto.class);
        if(request.getWareHouseID() != null ) {
            Warehouse warehouse = warehouseRepository.getOne(request.getWareHouseID());
            orderItemResponseDto.setWareHouseName(warehouse.getName());
        }

        return orderItemResponseDto;
    }

    public OrderItemResponseDto findOrderItem(Long id){
        OrderItem orderItem  = orderItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested order item Id does not exist!"));
        return mapper.map(orderItem, OrderItemResponseDto.class);
    }


    public Page<OrderItem> findAll(Long wareHouseID, String referenceNo, String deliveryStatus, Long partnerAssetID,
                                   String name, Integer qty, PageRequest pageRequest ){

        GenericSpecification<OrderItem> genericSpecification = new GenericSpecification<OrderItem>();

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
            genericSpecification.add(new SearchCriteria("deliveryStatus", deliveryStatus, SearchOperation.EQUAL));
        }

        if (partnerAssetID != null)
        {
            genericSpecification.add(new SearchCriteria("partnerAssetID", partnerAssetID, SearchOperation.EQUAL));
        }

        if (name != null && !name.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("name", name, SearchOperation.MATCH));
        }
        if (qty != null)
        genericSpecification.add(new SearchCriteria("qty", qty, SearchOperation.EQUAL));




        Page<OrderItem> orderItems = orderItemRepository.findAll(genericSpecification,pageRequest);
        if(orderItems == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
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
        List<OrderItem> Colors = orderItemRepository.findByIsActive(isActive);
        return Colors;

    }

    public Page<OrderItem> getAllDeliveries(Long partnerID, String deliveryStatus, PageRequest pageRequest ){
        GenericSpecification<OrderItem> genericSpecification = new GenericSpecification<OrderItem>();

        Warehouse warehouse = warehouseRepository.findByPartnerId(partnerID);

        if (warehouse.getId() != null)
        {
            genericSpecification.add(new SearchCriteria("wareHouseID", warehouse.getId(), SearchOperation.EQUAL));
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
