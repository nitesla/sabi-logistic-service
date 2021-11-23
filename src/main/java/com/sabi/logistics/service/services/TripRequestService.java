package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.TripRequestDto;
import com.sabi.logistics.core.dto.response.TripResponseDto;
import com.sabi.logistics.core.models.*;
import com.sabi.logistics.service.helper.GenericSpecification;
import com.sabi.logistics.service.helper.SearchCriteria;
import com.sabi.logistics.service.helper.SearchOperation;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.*;
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
public class TripRequestService {
    private final TripRequestRepository tripRequestRepository;
    private final ModelMapper mapper;
    @Autowired
    private Validations validations;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private PartnerAssetRepository partnerAssetRepository;

    @Autowired
    private TripItemRepository tripItemRepository;

    @Autowired
    private TripRequestResponseRepository tripRequestResponseRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    public TripRequestService(TripRequestRepository tripRequestRepository, ModelMapper mapper) {
        this.tripRequestRepository = tripRequestRepository;
        this.mapper = mapper;
    }

    public TripResponseDto createTripRequest(TripRequestDto request) {
        validations.validateTripRequest(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        TripRequest tripRequest = mapper.map(request,TripRequest.class);

        tripRequest.setReferenceNo(validations.generateReferenceNumber(10));
//        TripRequest tripRequestExists = tripRequestRepository.findByPartnerAssetIDAndPartnerID(tripRequest.getPartnerID(), tripRequest.getPartnerAssetID());
//        if(tripRequestExists != null){
//            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Trip Request already exist");
//        }

        tripRequest.setBarCode(validations.generateCode(tripRequest.getReferenceNo()));
        tripRequest.setQrCode(validations.generateCode(tripRequest.getReferenceNo()));

        Driver driver = driverRepository.findByUserId(request.getDriverID());
        User user = userRepository.getOne(driver.getUserId());
        tripRequest.setDriverID(driver.getId());

        tripRequest.setCreatedBy(userCurrent.getId());
        tripRequest.setIsActive(true);
        tripRequest = tripRequestRepository.save(tripRequest);
        log.debug("Create new trip Request - {}"+ new Gson().toJson(tripRequest));
        TripResponseDto tripResponseDto = mapper.map(tripRequest, TripResponseDto.class);

        if ((request.getPartnerAssetID() != null || request.getPartnerID() != null)) {
            Partner partner = partnerRepository.getOne(request.getPartnerID());
            PartnerAsset partnerAsset = partnerAssetRepository.getOne(request.getPartnerAssetID());
//            Driver driver = driverRepository.findByUserId(request.getDriverID());
//            User user = userRepository.getOne(driver.getUserId());

            tripResponseDto.setPartnerName(partner.getName());
            tripResponseDto.setPartnerAssetName(partnerAsset.getName());
            tripResponseDto.setDriverName(user.getLastName() + " " + user.getFirstName());
        }
        return tripResponseDto;
    }

    public TripResponseDto updateTripRequest(TripRequestDto request) {
        validations.validateTripRequest(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        TripRequest tripRequest = tripRequestRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Trip Request ID does not exist!"));
        mapper.map(request, tripRequest);
        if (request.getDriverID() != null) {

            Driver driver = driverRepository.findByUserId(request.getDriverID());
            User user = userRepository.getOne(driver.getUserId());
            tripRequest.setDriverID(driver.getId());
            tripRequest.setDriverName(user.getLastName() + " " + user.getFirstName());


        }
        tripRequest.setUpdatedBy(userCurrent.getId());
        tripRequestRepository.save(tripRequest);
        log.debug("tripRequest record updated - {}"+ new Gson().toJson(tripRequest));
        TripResponseDto tripResponseDto = mapper.map(tripRequest, TripResponseDto.class);

        if(request.getPartnerID() != null || request.getPartnerAssetID() != null ) {
            Partner partner = partnerRepository.getOne(request.getPartnerID());
            tripResponseDto.setPartnerName(partner.getName());
            PartnerAsset partnerAsset = partnerAssetRepository.getOne(request.getPartnerAssetID());
            tripResponseDto.setPartnerAssetName(partnerAsset.getName());
//            Driver driver = driverRepository.findByUserId(request.getDriverID());
//            User user = userRepository.getOne(driver.getUserId());
//            tripResponseDto.setDriverName(user.getLastName() + " " + user.getFirstName());
        }
        return tripResponseDto;
    }

    public TripResponseDto findTripRequest(Long id){
        TripRequest tripRequest  = tripRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Trip Request Id does not exist!"));

        TripResponseDto tripResponseDto = mapper.map(tripRequest, TripResponseDto.class);
        tripResponseDto.setTripItem(getAllTripItems(id));
        tripResponseDto.setTripRequestResponse(getAllRequestResponse(id));
        tripResponseDto.setDropOff(tripResponseDto.getTripItem().size());
        Partner partner = partnerRepository.getOne(tripResponseDto.getPartnerID());
        tripResponseDto.setPartnerName(partner.getName());
        PartnerAsset partnerAsset = partnerAssetRepository.getOne(tripResponseDto.getPartnerAssetID());
        tripResponseDto.setPartnerAssetName(partnerAsset.getName());
        Driver driver = driverRepository.getOne(tripResponseDto.getDriverID());
        User user = userRepository.getOne(driver.getUserId());
        tripResponseDto.setDriverName(user.getLastName() + " " + user.getFirstName());


        return tripResponseDto;
    }


    public Page<TripRequest> findAll(Long partnerID, String status, String referenceNo, Long driverID,
                                     Long wareHouseId, String wareHouseAddress, Long partnerAssetID, PageRequest pageRequest ){
        GenericSpecification<TripRequest> genericSpecification = new GenericSpecification<TripRequest>();

        if (partnerID != null)
        {
            genericSpecification.add(new SearchCriteria("partnerID", partnerID, SearchOperation.EQUAL));
        }

        if (status != null && !status.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("status", status, SearchOperation.MATCH));
        }

        if (referenceNo != null && !referenceNo.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("referenceNo", referenceNo, SearchOperation.MATCH));
        }

        if (driverID != null)
        {
            genericSpecification.add(new SearchCriteria("driverID", driverID, SearchOperation.EQUAL));
        }

        if (wareHouseId != null)
        {
            genericSpecification.add(new SearchCriteria("wareHouseId", wareHouseId, SearchOperation.EQUAL));
        }

        if (wareHouseAddress != null && !wareHouseAddress.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("wareHouseAddress", wareHouseAddress, SearchOperation.MATCH));
        }

        if (partnerAssetID != null)
        {
            genericSpecification.add(new SearchCriteria("partnerAssetID", partnerAssetID, SearchOperation.EQUAL));
        }




        Page<TripRequest> tripRequests = tripRequestRepository.findAll(genericSpecification, pageRequest);
        if(tripRequests == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        tripRequests.getContent().forEach(request ->{
            Partner partner = partnerRepository.getOne(request.getPartnerID());
            request.setPartnerName(partner.getName());
            PartnerAsset partnerAsset = partnerAssetRepository.getOne(request.getPartnerAssetID());
            request.setPartnerAssetName(partnerAsset.getName());
            Driver driver = driverRepository.getOne(request.getDriverID());
            User user = userRepository.getOne(driver.getUserId());
            request.setDriverName(user.getLastName() + " " + user.getFirstName());


        });
        return tripRequests;

    }



    public void enableDisable (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        TripRequest tripRequest  = tripRequestRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Trip Request Id does not exist!"));
        tripRequest.setIsActive(request.isActive());
        tripRequest.setUpdatedBy(userCurrent.getId());
        tripRequestRepository.save(tripRequest);

    }


    public List<TripRequest> getAll(Boolean isActive){
        List<TripRequest> tripRequests = tripRequestRepository.findByIsActive(isActive);
        for (TripRequest request : tripRequests) {
            Partner partner = partnerRepository.getOne(request.getPartnerID());
            request.setPartnerName(partner.getName());
            PartnerAsset partnerAsset = partnerAssetRepository.getOne(request.getPartnerAssetID());
            request.setPartnerAssetName(partnerAsset.getName());
            Driver driver = driverRepository.getOne(request.getDriverID());
            User user = userRepository.getOne(driver.getUserId());
            request.setDriverName(user.getLastName() + " " + user.getFirstName());
        }
        return tripRequests;

    }

    public List<TripItem> getAllTripItems(Long tripRequestID){
        List<TripItem> tripItems = tripItemRepository.findByTripRequestID(tripRequestID);
        for (TripItem item : tripItems) {
            OrderItem orderItem = orderItemRepository.getOne(item.getOrderItemID());

            Order order = orderRepository.getOne(orderItem.getOrderID());

            item.setOrderItemName(orderItem.getName());
            item.setQty(orderItem.getQty());
            item.setDeliveryAddress(order.getDeliveryAddress());
            item.setCustomerName(order.getCustomerName());
            item.setCustomerPhone(order.getCustomerPhone());

        }
        return tripItems;

    }
    public List<TripRequestResponse> getAllRequestResponse(Long tripRequestID){
        List<TripRequestResponse> tripRequests = tripRequestResponseRepository.findByTripRequestID(tripRequestID);
        return tripRequests;

    }
}
