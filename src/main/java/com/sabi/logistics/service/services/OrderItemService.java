package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.OrderItemRequestDto;
import com.sabi.logistics.core.dto.response.OrderItemResponseDto;
import com.sabi.logistics.core.models.OrderItem;
import com.sabi.logistics.core.models.PartnerAsset;
import com.sabi.logistics.service.helper.GenericSpecification;
import com.sabi.logistics.service.helper.SearchCriteria;
import com.sabi.logistics.service.helper.SearchOperation;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.OrderItemRepository;
import com.sabi.logistics.service.repositories.PartnerAssetRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class OrderItemService {
    private final OrderItemRepository orderItemRepository;
    private final ModelMapper mapper;
    @Autowired
    private Validations validations;
    @Autowired
    private PartnerAssetRepository partnerAssetRepository;


    public OrderItemService(OrderItemRepository orderItemRepository, ModelMapper mapper) {
        this.orderItemRepository = orderItemRepository;
        this.mapper = mapper;
    }

    public OrderItemResponseDto createOrderItem(OrderItemRequestDto request) {
        validations.validateOrderItem(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        OrderItem orderItem = mapper.map(request,OrderItem.class);

        orderItem.setReferenceNo(validations.generateReferenceNumber(10));
        OrderItem orderItemExists = orderItemRepository.findByReferenceNo(orderItem.getReferenceNo());
        if(orderItem.getReferenceNo() == null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Order Item does not have Reference Number");
        }

        if(orderItemExists !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Order Item already exist");
        }

        PartnerAsset partnerAsset = partnerAssetRepository.getOne(request.getPartnerAssetID());
        orderItem.setPartnerAssetName(partnerAsset.getName());

        orderItem.setCreatedBy(userCurrent.getId());
        orderItem.setIsActive(true);
        orderItem = orderItemRepository.save(orderItem);
        log.debug("Create new order item - {}"+ new Gson().toJson(orderItem));
        return mapper.map(orderItem, OrderItemResponseDto.class);
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
        return mapper.map(orderItem, OrderItemResponseDto.class);
    }

    public OrderItemResponseDto findOrderItem(Long id){
        OrderItem orderItem  = orderItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested order item Id does not exist!"));
        return mapper.map(orderItem, OrderItemResponseDto.class);
    }


    public Page<OrderItem> findAll(String referenceNo, String deliveryStatus, Long partnerAssetID,
                                   String name, Integer qty, PageRequest pageRequest ){

        GenericSpecification<OrderItem> genericSpecification = new GenericSpecification<OrderItem>();

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
}
