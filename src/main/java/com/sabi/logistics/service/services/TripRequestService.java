package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.TripMasterRequestDto;
import com.sabi.logistics.core.dto.request.TripRequestDto;
import com.sabi.logistics.core.dto.request.TripRequestResponseReqDto;
import com.sabi.logistics.core.dto.response.DropOffResponseDto;
import com.sabi.logistics.core.dto.response.TripMasterResponseDto;
import com.sabi.logistics.core.dto.response.TripRequestStatusCountResponse;
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

import java.util.ArrayList;
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
    private DropOffRepository dropOffRepository;

    @Autowired
    private TripItemRepository tripItemRepository;

    @Autowired
    private DropOffItemRepository dropOffItemRepository;


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

    @Autowired
    private TripRequestResponseService tripRequestResponseService;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private DropOffService dropOffService;

    @Autowired
    private DropOffItemService dropOffItemService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private DashboardSummaryRepository dashboardSummaryRepository;

    public TripRequestService(TripRequestRepository tripRequestRepository, ModelMapper mapper) {
           this.tripRequestRepository = tripRequestRepository;
        this.mapper = mapper;
    }

    public TripResponseDto createTripRequest(TripRequestDto request) {
        validations.validateTripRequest(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        TripRequest tripRequest = mapper.map(request,TripRequest.class);

        tripRequest.setReferenceNo(validations.generateReferenceNumber(10));

        TripRequest exist = tripRequestRepository.findByPartnerIdAndReferenceNo(request.getPartnerId(), tripRequest.getReferenceNo());
        if(exist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Trip Request already exist");
        }

        tripRequest.setBarCode(validations.generateCode(tripRequest.getReferenceNo()));
        tripRequest.setQrCode(validations.generateCode(tripRequest.getReferenceNo()));

        if (request.getDriverUserId() != null) {

            Driver driver = driverRepository.findByUserId(request.getDriverUserId());
            if (driver == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Driver Id");
            }
            User user = userRepository.getOne(driver.getUserId());
            tripRequest.setDriverId(driver.getId());
            tripRequest.setDriverUserId(driver.getUserId());
            tripRequest.setDriverName(user.getLastName() + " " + user.getFirstName());

        }
        if (request.getDriverAssistantUserId() != null) {
            Driver driver2 = driverRepository.findByUserId(request.getDriverAssistantUserId());
            if (driver2 == null) {
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Driver Assistant Id");
            }
            User user2 = userRepository.getOne(driver2.getUserId());
            tripRequest.setDriverAssistantId(driver2.getId());

            tripRequest.setDriverAssistantUserId(driver2.getUserId());

            tripRequest.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());

        }

        tripRequest.setCreatedBy(userCurrent.getId());
        tripRequest.setIsActive(true);
        tripRequest = tripRequestRepository.save(tripRequest);
        log.debug("Create new trip Request - {}"+ new Gson().toJson(tripRequest));
        TripResponseDto tripResponseDto = mapper.map(tripRequest, TripResponseDto.class);


        PartnerAsset partnerAsset = partnerAssetRepository.findPartnerAssetById(request.getPartnerAssetId());
        if ((request.getPartnerAssetId() != null || request.getPartnerId() != null)) {
            Partner partner = partnerRepository.findPartnerById(request.getPartnerId());
            if (partner == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid Partner Id");
            }
            if (partnerAsset == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid PartnerAsset Id");
            }

            tripResponseDto.setPartnerName(partner.getName());
            tripResponseDto.setPartnerAssetName(partnerAsset.getName());
        }

        DashboardSummary dashboardSummary = DashboardSummary.builder()
                .assetTypeId(partnerAsset.getPartnerAssetTypeId())
                .partnerId(tripRequest.getPartnerId())
                .date(tripRequest.getCreatedDate())
                .deliveryStatus(tripRequest.getDeliveryStatus())
                .totalEarnings(tripRequest.getEarnings())
                .build();
             dashboardSummaryRepository.save(dashboardSummary);
        return tripResponseDto;
    }

    public TripMasterResponseDto createMasterTripRequest(TripMasterRequestDto request) {
        List<DropOffResponseDto> dropOffResponseDtos = new ArrayList<>();

        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        TripRequest tripRequest = mapper.map(request,TripRequest.class);

        tripRequest.setReferenceNo(validations.generateReferenceNumber(10));

        TripRequest exist = tripRequestRepository.findByPartnerIdAndReferenceNo(request.getPartnerId(), tripRequest.getReferenceNo());
        if(exist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Trip Request already exist");
        }

        tripRequest.setBarCode(validations.generateCode(tripRequest.getReferenceNo()));
        tripRequest.setQrCode(validations.generateCode(tripRequest.getReferenceNo()));

        if (request.getDriverUserId() != null) {

            Driver driver = driverRepository.findByUserId(request.getDriverUserId());
            if (driver == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Driver Id");
            }
            User user = userRepository.getOne(driver.getUserId());
            tripRequest.setDriverId(driver.getId());
            tripRequest.setDriverUserId(driver.getUserId());
            tripRequest.setDriverName(user.getLastName() + " " + user.getFirstName());

        }
        if (request.getDriverAssistantUserId() != null) {
            Driver driver2 = driverRepository.findByUserId(request.getDriverAssistantUserId());
            if (driver2 == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Driver Assistant Id");
            }
            User user2 = userRepository.getOne(driver2.getUserId());
            tripRequest.setDriverAssistantId(driver2.getId());

            tripRequest.setDriverAssistantUserId(driver2.getUserId());

            tripRequest.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());

        }

        tripRequest.setCreatedBy(userCurrent.getId());
        tripRequest.setIsActive(true);
        tripRequest = tripRequestRepository.save(tripRequest);
        log.debug("Create new trip Request - {}"+ new Gson().toJson(tripRequest));
        TripMasterResponseDto tripResponseDto = mapper.map(tripRequest, TripMasterResponseDto.class);

        if ((request.getPartnerAssetId() != null || request.getPartnerId() != null)) {
            Partner partner = partnerRepository.findPartnerById(request.getPartnerId());
            if (partner == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid Partner Id");
            }
            PartnerAsset partnerAsset = partnerAssetRepository.findPartnerAssetById(request.getPartnerAssetId());
            if (partnerAsset == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid PartnerAsset Id");
            };

            tripResponseDto.setPartnerName(partner.getName());
            tripResponseDto.setPartnerAssetName(partnerAsset.getName());
        }

        if(request.getDropOff() != null) {
            dropOffResponseDtos = dropOffService.createDropOffs(request.getDropOff(), tripResponseDto.getId());
            List<DropOffResponseDto> finalDropOffResponse = dropOffResponseDtos;
            dropOffResponseDtos.forEach(response -> {
                tripResponseDto.setDropOff(finalDropOffResponse);
            });
        }

        return tripResponseDto;
    }

    public TripResponseDto updateTripRequest(TripRequestDto request) {
        validations.validateTripRequest(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        TripRequest tripRequest = tripRequestRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Trip Request ID does not exist!"));
        TripRequestResponse tripRequestResponse = new TripRequestResponse();
        TripRequestResponseReqDto tripRequestResponseReqDto = new TripRequestResponseReqDto();

        if(tripRequest.getStatus() != request.getStatus())
        {
            if (request.getStatus().equalsIgnoreCase("Pending") || tripRequest.getStatus().equalsIgnoreCase("Pending")) {
                tripRequestResponse = tripRequestResponseRepository.findTripRequestResponseByTripRequestId(tripRequest.getId());
                if (tripRequestResponse == null) {
                    tripRequestResponseReqDto.setTripRequestId(tripRequest.getId());
                    tripRequestResponseReqDto.setPartnerId(tripRequest.getPartnerId());
                    tripRequestResponseReqDto.setResponseDate(tripRequest.getUpdatedDate().now());
                    tripRequestResponseReqDto.setStatus(request.getStatus());
                    tripRequestResponseReqDto.setRejectReason(request.getRejectReason());
                    tripRequestResponseService.createTripRequestResponse(tripRequestResponseReqDto);
                }
            }else if(request.getStatus().equalsIgnoreCase("Rejected")){
                tripRequestResponse = tripRequestResponseRepository.findTripRequestResponseByTripRequestId(tripRequest.getId());
                tripRequestResponseReqDto.setTripRequestId(tripRequest.getId());
                tripRequestResponseReqDto.setPartnerId(tripRequest.getPartnerId());
                tripRequestResponseReqDto.setResponseDate(tripRequest.getUpdatedDate().now());
                tripRequestResponseReqDto.setStatus(request.getStatus());
                tripRequestResponseReqDto.setRejectReason(request.getRejectReason());
                tripRequestResponseReqDto.setId(tripRequestResponse.getId());
                tripRequestResponseService.updateTripRequestResponse(tripRequestResponseReqDto);
            }
            if(request.getStatus().equalsIgnoreCase("Accepted")) {
                tripRequestResponse = tripRequestResponseRepository.findTripRequestResponseByTripRequestId(tripRequest.getId());
                if (tripRequestResponse != null) {
                    tripRequestResponseReqDto.setTripRequestId(tripRequest.getId());
                    tripRequestResponseReqDto.setPartnerId(tripRequest.getPartnerId());
                    tripRequestResponseReqDto.setResponseDate(tripRequest.getUpdatedDate().now());
                    tripRequestResponseReqDto.setStatus(request.getStatus());
                    tripRequestResponseReqDto.setRejectReason(request.getRejectReason());
                    tripRequestResponseReqDto.setId(tripRequestResponse.getId());
                    tripRequestResponseService.updateTripRequestResponse(tripRequestResponseReqDto);
                }
            }
        }

        if (request.getStatus().equalsIgnoreCase("Rejected")){
            request.setStatus("Pending");
            request.setWareHouseId(0l);
            mapper.map(request, tripRequest);
        }else {
            mapper.map(request, tripRequest);
        }
        if (request.getDriverUserId() != null) {

            Driver driver = driverRepository.findByUserId(request.getDriverUserId());

            User user = userRepository.getOne(driver.getUserId());
            tripRequest.setDriverId(driver.getId());
            tripRequest.setDriverUserId(driver.getUserId());
            tripRequest.setDriverName(user.getLastName() + " " + user.getFirstName());
        }
        if (request.getDriverAssistantUserId() != null) {

            Driver driver2 = driverRepository.findByUserId(request.getDriverAssistantUserId());

            User user2 = userRepository.getOne(driver2.getUserId());
            tripRequest.setDriverAssistantId(driver2.getId());
            tripRequest.setDriverAssistantUserId(driver2.getUserId());
            tripRequest.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());

        }
        tripRequest.setUpdatedBy(userCurrent.getId());
        tripRequestRepository.save(tripRequest);
        log.debug("tripRequest record updated - {}"+ new Gson().toJson(tripRequest));
        TripResponseDto tripResponseDto = mapper.map(tripRequest, TripResponseDto.class);

        if(request.getPartnerId() != null || request.getPartnerAssetId() != null ) {
            Partner partner = partnerRepository.findPartnerById(request.getPartnerId());
            if (partner == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid Partner Id");
            }
            tripResponseDto.setPartnerName(partner.getName());
            PartnerAsset partnerAsset = partnerAssetRepository.findPartnerAssetById(request.getPartnerAssetId());
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
        tripResponseDto.setDropOff(getAllDropOffs(id));
        tripResponseDto.setDropOffCount(getDropOff(id));
        Partner partner = partnerRepository.findPartnerById(tripResponseDto.getPartnerId());
        if (partner == null) {
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid Partner Id");
        }
        tripResponseDto.setPartnerName(partner.getName());
        PartnerAsset partnerAsset = partnerAssetRepository.findPartnerAssetById(tripResponseDto.getPartnerAssetId());
        if (partnerAsset == null) {
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid PartnerAsset Id");
        }
        tripResponseDto.setPartnerAssetName(partnerAsset.getName());
        Driver driver = driverRepository.findDriverById(tripResponseDto.getDriverId());

        User user = userRepository.getOne(driver.getUserId());
        tripResponseDto.setDriverName(user.getLastName() + " " + user.getFirstName());

        Driver driver2 = driverRepository.findDriverById(tripResponseDto.getDriverAssistantId());

        User user2 = userRepository.getOne(driver2.getUserId());
        tripResponseDto.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());

        Warehouse warehouse = warehouseRepository.findWarehouseById(tripResponseDto.getWareHouseId());
        if (warehouse == null) {
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid warehouse Id");
        };
        tripResponseDto.setWareHouseAddress(tripResponseDto.getWareHouseAddress());
        tripResponseDto.setContactPerson(warehouse.getContactPerson());
        tripResponseDto.setContactEmail(warehouse.getContactEmail());
        tripResponseDto.setContactPhone(warehouse.getContactPhone());


        return tripResponseDto;
    }


    public Page<TripRequest> findAll(Long partnerId, String status, String referenceNo, Long driverUserId,
                                     Long wareHouseId, String wareHouseAddress, Long partnerAssetId, PageRequest pageRequest ){
        GenericSpecification<TripRequest> genericSpecification = new GenericSpecification<TripRequest>();

        if (partnerId != null)
        {
            genericSpecification.add(new SearchCriteria("partnerId", partnerId, SearchOperation.EQUAL));
        }

        if (status != null && !status.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("status", status, SearchOperation.MATCH));
        }

        if (referenceNo != null && !referenceNo.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("referenceNo", referenceNo, SearchOperation.MATCH));
        }

        Driver driver = driverRepository.findByUserId(driverUserId);

        Long driverId = driver.getId();

        if (driverId != null)
        {
            genericSpecification.add(new SearchCriteria("driverId", driverId, SearchOperation.EQUAL));
        }

        if (wareHouseId != null)
        {
            genericSpecification.add(new SearchCriteria("wareHouseId", wareHouseId, SearchOperation.EQUAL));
        }

        if (wareHouseAddress != null && !wareHouseAddress.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("wareHouseAddress", wareHouseAddress, SearchOperation.MATCH));
        }

        if (partnerAssetId != null)
        {
            genericSpecification.add(new SearchCriteria("partnerAssetId", partnerAssetId, SearchOperation.EQUAL));
        }




        Page<TripRequest> tripRequests = tripRequestRepository.findAll(genericSpecification, pageRequest);
        if(tripRequests == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        tripRequests.getContent().forEach(request ->{

            Partner partner = partnerRepository.findPartnerById(request.getPartnerId());
            if (partner == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid Partner Id");
            }
            PartnerAsset partnerAsset = partnerAssetRepository.findPartnerAssetById(request.getPartnerAssetId());
//            if (partnerAsset == null) {
//                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid PartnerAsset Id");
//            };
            Driver driver1 = driverRepository.findDriverById(request.getDriverId());

            User user = userRepository.getOne(driver1.getUserId());

            Driver driver2 = driverRepository.findDriverById(request.getDriverAssistantId());

            User user2 = userRepository.getOne(driver2.getUserId());

            request.setDropOffCount(getDropOff(request.getId()));

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
            Partner partner = partnerRepository.findPartnerById(request.getPartnerId());
            if (partner == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid Partner Id");
            }
            request.setPartnerName(partner.getName());
            PartnerAsset partnerAsset = partnerAssetRepository.findPartnerAssetById(request.getPartnerAssetId());
            if (partnerAsset == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid PartnerAsset Id");
            };
            request.setPartnerAssetName(partnerAsset.getName());
            Driver driver = driverRepository.findDriverById(request.getDriverId());

            User user = userRepository.getOne(driver.getUserId());
            request.setDriverName(user.getLastName() + " " + user.getFirstName());

            Driver driver2 = driverRepository.findDriverById(request.getDriverAssistantId());

            User user2 = userRepository.getOne(driver2.getUserId());
            request.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());

            request.setDropOffCount(getDropOff(request.getId()));


        }
        return tripRequests;

    }

    public List<TripItem> getAllTripItems(Long tripRequestId){
        List<TripItem> tripItems = tripItemRepository.findByTripRequestId(tripRequestId);

        return tripItems;

    }

    public List<DropOff> getAllDropOffs(Long tripRequestId){
        List<DropOff> dropOffs = dropOffRepository.findByTripRequestId(tripRequestId);

        for (DropOff dropOff : dropOffs) {

            Order order = orderRepository.getOne(dropOff.getOrderId());
            dropOff.setCustomerName(order.getCustomerName());
            dropOff.setDeliveryAddress(order.getDeliveryAddress());
            dropOff.setCustomerPhone(order.getCustomerPhone());

            dropOff.setDropOffItem(getAllDropOffItems(dropOff.getId()));
        }



        return dropOffs;

    }

    public List<TripRequestResponse> getAllRequestResponse(Long tripRequestId){
        List<TripRequestResponse> tripRequests = tripRequestResponseRepository.findByTripRequestId(tripRequestId);
        return tripRequests;

    }

    public Integer getDropOff(Long tripRequestId){
        Integer dropOff = dropOffRepository.countByTripRequestId(tripRequestId);

        return dropOff;

    }

    public List<DropOffItem> getAllDropOffItems(Long dropOffId){
        List<DropOffItem> dropOffItems = dropOffItemRepository.findByDropOffId(dropOffId);

        for (DropOffItem dropOffItem : dropOffItems) {

            OrderItem orderItem = orderItemRepository.getOne(dropOffItem.getOrderItemId());
            Order order = orderRepository.getOne(orderItem.getOrderId());
            dropOffItem.setCustomerName(order.getCustomerName());
            dropOffItem.setCustomerPhone(order.getCustomerPhone());
        }


        return dropOffItems;

    }

    public Page<TripRequest> getDeliveries(Long partnerId, String deliveryStatus,
                                      Long partnerAssetId, PageRequest pageRequest ){
        GenericSpecification<TripRequest> genericSpecification = new GenericSpecification<TripRequest>();

        if (partnerId != null)
        {
            genericSpecification.add(new SearchCriteria("partnerId", partnerId, SearchOperation.EQUAL));
        }

        if (deliveryStatus != null)
        {
            genericSpecification.add(new SearchCriteria("deliveryStatus", deliveryStatus, SearchOperation.MATCH));
        }


        if (partnerAssetId != null)
        {
            genericSpecification.add(new SearchCriteria("partnerAssetId", partnerAssetId, SearchOperation.EQUAL));
        }

        String status = "Accepted";
        genericSpecification.add(new SearchCriteria("status", status, SearchOperation.MATCH));

        Page<TripRequest> tripRequests = tripRequestRepository.findAll(genericSpecification, pageRequest);
        if (tripRequests == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No Accepted Delivery Available!");
        }

        return tripRequests;

    }

    public TripRequestStatusCountResponse getStatus(Long driverUserId){
        Driver driver = driverRepository.findByUserId(driverUserId);
        Integer pendingCount = tripRequestRepository.countByDriverIdAndStatus(driver.getId(),"Pending");
        Integer AcceptedCount = tripRequestRepository.countByDriverIdAndStatus(driver.getId(),"Accepted");
        Integer RejectedCount = tripRequestRepository.countByDriverIdAndStatus(driver.getId(),"Rejected");
        TripRequestStatusCountResponse response = new TripRequestStatusCountResponse();
         response.setPending(pendingCount);
        response.setAccepted(AcceptedCount);
        response.setRejected(RejectedCount);
        return response;

    }
}
