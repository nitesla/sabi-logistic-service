package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
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


        tripRequest.setBarCode(validations.generateCode(tripRequest.getReferenceNo()));
        tripRequest.setQrCode(validations.generateCode(tripRequest.getReferenceNo()));

        if (request.getDriverID() != null) {

            Driver driver = driverRepository.findByUserId(request.getDriverID());
            if (driver == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Driver Id");
            }
            User user = userRepository.getOne(driver.getUserId());
            tripRequest.setDriverID(driver.getId());
            tripRequest.setDriverName(user.getLastName() + " " + user.getFirstName());

        }
        if (request.getDriverAssistantID() != null) {
            Driver driver2 = driverRepository.findByUserId(request.getDriverAssistantID());
            if (driver2 == null) {
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Driver Assistant Id");
            }
            User user2 = userRepository.getOne(driver2.getUserId());
            tripRequest.setDriverAssistantID(driver2.getId());


            tripRequest.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());

        }

        tripRequest.setCreatedBy(userCurrent.getId());
        tripRequest.setIsActive(true);
        tripRequest = tripRequestRepository.save(tripRequest);
        log.debug("Create new trip Request - {}"+ new Gson().toJson(tripRequest));
        TripResponseDto tripResponseDto = mapper.map(tripRequest, TripResponseDto.class);

        if ((request.getPartnerAssetID() != null || request.getPartnerID() != null)) {
            Partner partner = partnerRepository.findPartnerById(request.getPartnerID());
            if (partner == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid Partner Id");
            }
            PartnerAsset partnerAsset = partnerAssetRepository.findPartnerAssetById(request.getPartnerAssetID());
            if (partnerAsset == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid PartnerAsset Id");
            };

            tripResponseDto.setPartnerName(partner.getName());
            tripResponseDto.setPartnerAssetName(partnerAsset.getName());
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
        if (request.getDriverAssistantID() != null) {

            Driver driver2 = driverRepository.findByUserId(request.getDriverAssistantID());

            User user2 = userRepository.getOne(driver2.getUserId());
            tripRequest.setDriverAssistantID(driver2.getId());
            tripRequest.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());

        }
        tripRequest.setUpdatedBy(userCurrent.getId());
        tripRequestRepository.save(tripRequest);
        log.debug("tripRequest record updated - {}"+ new Gson().toJson(tripRequest));
        TripResponseDto tripResponseDto = mapper.map(tripRequest, TripResponseDto.class);

        if(request.getPartnerID() != null || request.getPartnerAssetID() != null ) {
            Partner partner = partnerRepository.findPartnerById(request.getPartnerID());
            if (partner == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid Partner Id");
            }
            tripResponseDto.setPartnerName(partner.getName());
            PartnerAsset partnerAsset = partnerAssetRepository.findPartnerAssetById(request.getPartnerAssetID());
            if (partnerAsset == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid PartnerAsset Id");
            };
            tripResponseDto.setPartnerAssetName(partnerAsset.getName());

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
        tripResponseDto.setDropOff(getDropOff(id));
        Partner partner = partnerRepository.findPartnerById(tripResponseDto.getPartnerID());
        if (partner == null) {
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid Partner Id");
        }
        tripResponseDto.setPartnerName(partner.getName());
        PartnerAsset partnerAsset = partnerAssetRepository.findPartnerAssetById(tripResponseDto.getPartnerAssetID());
        if (partnerAsset == null) {
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid PartnerAsset Id");
        };
        tripResponseDto.setPartnerAssetName(partnerAsset.getName());
        Driver driver = driverRepository.findDriverById(tripResponseDto.getDriverID());

        User user = userRepository.getOne(driver.getUserId());
        tripResponseDto.setDriverName(user.getLastName() + " " + user.getFirstName());

        Driver driver2 = driverRepository.findDriverById(tripResponseDto.getDriverAssistantID());

        User user2 = userRepository.getOne(driver2.getUserId());
        tripResponseDto.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());
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

            Partner partner = partnerRepository.findPartnerById(request.getPartnerID());
            if (partner == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid Partner Id");
            }
            PartnerAsset partnerAsset = partnerAssetRepository.findPartnerAssetById(request.getPartnerAssetID());
//            if (partnerAsset == null) {
//                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid PartnerAsset Id");
//            };
            Driver driver = driverRepository.findDriverById(request.getDriverID());

            User user = userRepository.getOne(driver.getUserId());

            Driver driver2 = driverRepository.findDriverById(request.getDriverAssistantID());

            User user2 = userRepository.getOne(driver2.getUserId());

            request.setDropOff(getDropOff(request.getId()));

            if ((partner.getName() != null || partnerAsset.getName() != null || user.getFirstName() != null || user.getLastName() != null
            || !(partner.getName().isEmpty() || partnerAsset.getName().isEmpty() || user.getFirstName().isEmpty() || user.getLastName().isEmpty()))) {
                request.setPartnerName(partner.getName());
                request.setPartnerAssetName(partnerAsset.getName());
                request.setDriverName(user.getLastName() + " " + user.getFirstName());
                request.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());
            }

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
            Partner partner = partnerRepository.findPartnerById(request.getPartnerID());
            if (partner == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid Partner Id");
            }
            request.setPartnerName(partner.getName());
            PartnerAsset partnerAsset = partnerAssetRepository.findPartnerAssetById(request.getPartnerAssetID());
            if (partnerAsset == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid PartnerAsset Id");
            };
            request.setPartnerAssetName(partnerAsset.getName());
            Driver driver = driverRepository.findDriverById(request.getDriverID());

            User user = userRepository.getOne(driver.getUserId());
            request.setDriverName(user.getLastName() + " " + user.getFirstName());

            Driver driver2 = driverRepository.findDriverById(request.getDriverAssistantID());

            User user2 = userRepository.getOne(driver2.getUserId());
            request.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());

            request.setDropOff(getDropOff(request.getId()));


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

    public Integer getDropOff(Long tripRequestID){
        Integer dropOff = tripItemRepository.countTripItemByTripRequestID(tripRequestID);

        return dropOff;

    }

    public Page<TripRequest> getDeliveries(Long partnerID, String deliveryStatus,
                                      Long partnerAssetID, PageRequest pageRequest ){
        GenericSpecification<TripRequest> genericSpecification = new GenericSpecification<TripRequest>();

        if (partnerID != null)
        {
            genericSpecification.add(new SearchCriteria("partnerID", partnerID, SearchOperation.EQUAL));
        }

        if (deliveryStatus != null)
        {
            genericSpecification.add(new SearchCriteria("deliveryStatus", deliveryStatus, SearchOperation.MATCH));
        }


        if (partnerAssetID != null)
        {
            genericSpecification.add(new SearchCriteria("partnerAssetID", partnerAssetID, SearchOperation.EQUAL));
        }

        String status = "Accepted";
        genericSpecification.add(new SearchCriteria("status", status, SearchOperation.MATCH));

        Page<TripRequest> tripRequests = tripRequestRepository.findAll(genericSpecification, pageRequest);
        if (tripRequests == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No Accepted Delivery Available!");
        }

        return tripRequests;

    }
}
