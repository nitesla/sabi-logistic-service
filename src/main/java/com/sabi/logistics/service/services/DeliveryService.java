package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.DeliveryRequestDto;
import com.sabi.logistics.core.dto.response.DeliveryResponseDto;
import com.sabi.logistics.core.models.Delivery;
import com.sabi.logistics.core.models.Driver;
import com.sabi.logistics.core.models.OrderItem;
import com.sabi.logistics.core.models.PartnerAsset;
import com.sabi.logistics.service.helper.GenericSpecification;
import com.sabi.logistics.service.helper.SearchCriteria;
import com.sabi.logistics.service.helper.SearchOperation;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.DeliveryRepository;
import com.sabi.logistics.service.repositories.DriverRepository;
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
public class DeliveryService {
    private final DeliveryRepository deliveryRepository;
    private final ModelMapper mapper;
    @Autowired
    private Validations validations;

    @Autowired
    private PartnerAssetRepository partnerAssetRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private DriverRepository driverRepository;


    public DeliveryService(DeliveryRepository deliveryRepository, ModelMapper mapper) {
        this.deliveryRepository = deliveryRepository;
        this.mapper = mapper;
    }

    public DeliveryResponseDto createDelivery(DeliveryRequestDto request) {
        validations.validateDelivery(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Delivery delivery = mapper.map(request,Delivery.class);

        Delivery deliveryExists = deliveryRepository.findByOrderItemIDAndPartnerAssetID(delivery.getOrderItemID(), delivery.getPartnerAssetID());


        if(deliveryExists != null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Delivery already exist");
        }

        PartnerAsset partnerAsset = partnerAssetRepository.getOne(request.getPartnerAssetID());
        delivery.setPartnerAssetName(partnerAsset.getName());

        OrderItem orderItem = orderItemRepository.getOne(request.getOrderItemID());
        delivery.setOrderItemName(orderItem.getName());

        Driver driver = driverRepository.getOne(request.getDriverID());
        delivery.setDriverName(driver.getName());


        delivery.setCreatedBy(userCurrent.getId());
        delivery.setIsActive(true);
        delivery = deliveryRepository.save(delivery);
        log.debug("Create new delivery - {}"+ new Gson().toJson(delivery));
        return mapper.map(delivery, DeliveryResponseDto.class);
    }

    public DeliveryResponseDto updateDelivery(DeliveryRequestDto request) {
        validations.validateDelivery(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Delivery delivery = deliveryRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested delivery Id does not exist!"));
        mapper.map(request, delivery);
        delivery.setUpdatedBy(userCurrent.getId());
        deliveryRepository.save(delivery);
        log.debug("delivery record updated - {}"+ new Gson().toJson(delivery));
        return mapper.map(delivery, DeliveryResponseDto.class);
    }

    public DeliveryResponseDto findDelivery(Long id){
        Delivery delivery  = deliveryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested delivery Id does not exist!"));
        return mapper.map(delivery, DeliveryResponseDto.class);
    }


    public Page<Delivery> findAll(Long partnerAssetID, Long orderItemID, String status, Long driverID,
                               Long driverAssistantID, PageRequest pageRequest ){
        GenericSpecification<Delivery> genericSpecification = new GenericSpecification<Delivery>();

        if (partnerAssetID != null)
        {
            genericSpecification.add(new SearchCriteria("partnerAssetID", partnerAssetID, SearchOperation.EQUAL));
        }

        if (orderItemID != null)
        {
            genericSpecification.add(new SearchCriteria("orderItemID", orderItemID, SearchOperation.EQUAL));
        }

        if (status != null && !status.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("status", status, SearchOperation.EQUAL));
        }

        if (driverID != null)
        {
            genericSpecification.add(new SearchCriteria("driverID", driverID, SearchOperation.EQUAL));
        }

        if (driverAssistantID != null)
        {
            genericSpecification.add(new SearchCriteria("driverAssistantID", driverAssistantID, SearchOperation.EQUAL));
        }



        Page<Delivery> deliverys = deliveryRepository.findAll(genericSpecification, pageRequest);
        if(deliverys == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return deliverys;

    }



    public void enableDisable (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Delivery delivery  = deliveryRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Delivery Id does not exist!"));
        delivery.setIsActive(request.isActive());
        delivery.setUpdatedBy(userCurrent.getId());
        deliveryRepository.save(delivery);

    }


    public List<Delivery> getAll(Boolean isActive){
        List<Delivery> deliverys = deliveryRepository.findByIsActive(isActive);
        return deliverys;

    }
}
