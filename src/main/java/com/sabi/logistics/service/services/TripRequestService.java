package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.service.NotificationService;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.service.WhatsAppService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.*;
import com.sabi.logistics.core.dto.response.DropOffResponseDto;
import com.sabi.logistics.core.dto.response.TripMasterResponseDto;
import com.sabi.logistics.core.dto.response.TripRequestStatusCountResponse;
import com.sabi.logistics.core.dto.response.TripResponseDto;
import com.sabi.logistics.core.enums.PaymentStatus;
import com.sabi.logistics.core.models.*;
import com.sabi.logistics.service.helper.GenericSpecification;
import com.sabi.logistics.service.helper.SearchCriteria;
import com.sabi.logistics.service.helper.SearchOperation;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("All")
@Service
@Slf4j
public class TripRequestService {

    @Value("${trip-request-due-for-expiry-db-load-time}")
    private String tripRequestDueForExpireTimeInMiliseconds;
    private final TripRequestRepository tripRequestRepository;
    private final ModelMapper mapper;
    private final NotificationService notificationService;

    private final WhatsAppService whatsAppService;
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
    private InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private TripRequestResponseService tripRequestResponseService;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private DropOffService dropOffService;

    @Autowired
    private DropOffItemService dropOffItemService;

    @Autowired
    private InvoiceItemService invoiceItemService;

    @Autowired
    private DashboardSummaryRepository dashboardSummaryRepository;

    private List<TripRequest> tripsDueForExpiration;

    private final GeneralNotificationService generalNotificationService;

    public TripRequestService(TripRequestRepository tripRequestRepository, ModelMapper mapper, NotificationService notificationService, WhatsAppService whatsAppService, GeneralNotificationService generalNotificationService) {
           this.tripRequestRepository = tripRequestRepository;
        this.mapper = mapper;
        this.notificationService = notificationService;
        this.whatsAppService = whatsAppService;
        this.generalNotificationService = generalNotificationService;
        this.tripsDueForExpiration = new ArrayList<>();
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

        if (request.getWareHouseId() != null) {
            Warehouse warehouse = warehouseRepository.findWarehouseById(request.getWareHouseId());
            if (warehouse == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid warehouse Id");
            }
            ;
            tripRequest.setWareHouseAddress(warehouse.getAddress());
            tripRequest.setContactPerson(warehouse.getContactPerson());
            tripRequest.setContactEmail(warehouse.getContactEmail());
            tripRequest.setContactPhone(warehouse.getContactPhone());
        }

        tripRequest.setCreatedBy(userCurrent.getId());
        tripRequest.setIsActive(true);
        tripRequest = tripRequestRepository.save(tripRequest);
        log.debug("Create new trip Request - {}"+ new Gson().toJson(tripRequest));
        TripResponseDto tripResponseDto = mapper.map(tripRequest, TripResponseDto.class);



        if (request.getPartnerId() != null) {
            Partner partner = partnerRepository.findPartnerById(request.getPartnerId());
            if (partner == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Partner Id");
            }
            log.info("setting expired Time");
            tripRequest.setExpiredTime(LocalDateTime.now().plusMinutes(Long.parseLong(tripRequestDueForExpireTimeInMiliseconds)/(60*1000)));
            tripRequest.setAssignedDate(LocalDateTime.now());
            tripResponseDto.setPartnerName(partner.getName());
            tripRequest.setDriverStatus("pending");
            tripRequest=tripRequestRepository.save(tripRequest);
            tripResponseDto.setExpiredTime(tripRequest.getExpiredTime());
            // Create Default TripRequestResponse for the trip
            TripRequestResponse tripRequestResponse = new TripRequestResponse();
            TripRequestResponseReqDto tripRequestResponseReqDto = new TripRequestResponseReqDto();
            tripRequestResponseReqDto.setTripRequestId(tripRequest.getId());
            tripRequestResponseReqDto.setPartnerId(tripRequest.getPartnerId());
            tripRequestResponseReqDto.setResponseDate(tripRequest.getUpdatedDate().now());
            tripRequestResponseReqDto.setStatus(request.getStatus());
            if(request.getRejectReason()!=null){
                tripRequestResponseReqDto.setRejectReason(request.getRejectReason());
            }
            tripRequestResponseService.createTripRequestResponse(tripRequestResponseReqDto);
        }

        if (request.getPartnerAssetId() != null ) {
            PartnerAsset partnerAsset = partnerAssetRepository.findPartnerAssetById(request.getPartnerAssetId());
            if (partnerAsset == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid PartnerAsset Id");
            }
            tripResponseDto.setPartnerAssetName(partnerAsset.getName());
        }
        return tripResponseDto;
    }

    @Transactional
    public TripMasterResponseDto createMasterTripRequest(TripMasterRequestDto request) {
        validations.validateMasterTripRequest(request);
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
        if (request.getWareHouseId() != null) {
            Warehouse warehouse = warehouseRepository.findWarehouseById(request.getWareHouseId());
            if (warehouse == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid warehouse Id");
            }
            ;
            tripRequest.setWareHouseAddress(warehouse.getAddress());
            tripRequest.setContactPerson(warehouse.getContactPerson());
            tripRequest.setContactEmail(warehouse.getContactEmail());
            tripRequest.setContactPhone(warehouse.getContactPhone());
        }

        tripRequest.setCreatedBy(userCurrent.getId());
        tripRequest.setIsActive(true);
        tripRequest.setDriverStatus("pending");
        tripRequest = tripRequestRepository.save(tripRequest);
        log.debug("Create new trip Request - {}"+ new Gson().toJson(tripRequest));
        TripMasterResponseDto tripResponseDto = mapper.map(tripRequest, TripMasterResponseDto.class);

        if (request.getPartnerId() != null) {
            Partner partner = partnerRepository.findPartnerById(request.getPartnerId());
            if (partner == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Partner Id");
            }
            log.info("setting expired Time");
            tripRequest.setExpiredTime(LocalDateTime.now().plusMinutes(Long.parseLong(tripRequestDueForExpireTimeInMiliseconds)/(60*1000)));
            tripRequest.setAssignedDate(LocalDateTime.now());
            tripRequest=tripRequestRepository.save(tripRequest);
            tripResponseDto.setExpiredTime(tripRequest.getExpiredTime());
            tripResponseDto.setPartnerName(partner.getName());
            // Create Default TripRequestResponse for the trip
            TripRequestResponse tripRequestResponse = new TripRequestResponse();
            TripRequestResponseReqDto tripRequestResponseReqDto = new TripRequestResponseReqDto();
            tripRequestResponseReqDto.setTripRequestId(tripRequest.getId());
            tripRequestResponseReqDto.setPartnerId(tripRequest.getPartnerId());
            tripRequestResponseReqDto.setResponseDate(tripRequest.getUpdatedDate().now());
            tripRequestResponseReqDto.setStatus(request.getStatus());
            if(request.getRejectReason()!=null){
                tripRequestResponseReqDto.setRejectReason(request.getRejectReason());
            }
            tripRequestResponseService.createTripRequestResponse(tripRequestResponseReqDto);
        }

        if (request.getPartnerAssetId() != null) {
            PartnerAsset partnerAsset = partnerAssetRepository.findPartnerAssetById(request.getPartnerAssetId());
            if (partnerAsset == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid PartnerAsset Id");
            }
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
        //validations.validateTripRequest(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        TripRequest tripRequest = tripRequestRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Trip Request ID does not exist!"));

        if (tripRequest.getPartnerId() == null) {
            tripRequest.setPartnerId(request.getPartnerId());
        }

        if (tripRequest.getPartnerAssetId() == null) {
            tripRequest.setPartnerAssetId(request.getPartnerAssetId());
        }

        TripRequestResponse tripRequestResponse = new TripRequestResponse();
        TripRequestResponseReqDto tripRequestResponseReqDto = new TripRequestResponseReqDto();

        if(tripRequest.getStatus() != request.getStatus())
        {
            if (request.getStatus().equalsIgnoreCase("pending") || tripRequest.getStatus().equalsIgnoreCase("pending")) {
                tripRequestResponse = tripRequestResponseRepository.findTripRequestResponseByTripRequestId(tripRequest.getId());
                if (tripRequestResponse == null) {
                    tripRequestResponseReqDto.setTripRequestId(tripRequest.getId());
                    tripRequestResponseReqDto.setPartnerId(tripRequest.getPartnerId());
                    tripRequestResponseReqDto.setResponseDate(tripRequest.getUpdatedDate().now());
                    tripRequestResponseReqDto.setStatus(request.getStatus());
                    tripRequestResponseReqDto.setRejectReason(request.getRejectReason());
                    tripRequestResponseService.createTripRequestResponse(tripRequestResponseReqDto);
                }
            }
            if(request.getStatus().equalsIgnoreCase("rejected")){
                tripRequestResponse = tripRequestResponseRepository.findTripRequestResponseByTripRequestId(tripRequest.getId());
                if (tripRequestResponse != null) {
                    tripRequestResponseReqDto.setTripRequestId(tripRequest.getId());
                    tripRequestResponseReqDto.setPartnerId(tripRequest.getPartnerId());
                    tripRequestResponseReqDto.setResponseDate(tripRequest.getUpdatedDate().now());
                    tripRequestResponseReqDto.setStatus(request.getStatus());
                    tripRequestResponseReqDto.setRejectReason(request.getRejectReason());
                    tripRequestResponseReqDto.setId(tripRequestResponse.getId());
                    tripRequestResponseService.updateTripRequestResponse(tripRequestResponseReqDto);
                }else if (tripRequestResponse == null) {
                    tripRequestResponseReqDto.setTripRequestId(tripRequest.getId());
                    tripRequestResponseReqDto.setPartnerId(tripRequest.getPartnerId());
                    tripRequestResponseReqDto.setResponseDate(tripRequest.getUpdatedDate().now());
                    tripRequestResponseReqDto.setStatus(request.getStatus());
                    tripRequestResponseReqDto.setRejectReason(request.getRejectReason());
                    tripRequestResponseService.createTripRequestResponse(tripRequestResponseReqDto);
                }
            }
            if(request.getStatus().equalsIgnoreCase("accepted")) {
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

        if (request.getStatus().equalsIgnoreCase("rejected")){
            request.setStatus("pending");
            tripRequest.setPartnerId(null);
            log.info("partnerId {}", tripRequest.getPartnerId());
            mapper.map(request, tripRequest);
        }else {
            mapper.map(request, tripRequest);
        }
        //Explicitly confirm it has been assigned to a partner and the partner is yet to accept it.
        if (tripRequest.getPartnerId()!=null && tripRequest.getStatus().equalsIgnoreCase("pending")){
            //Trigger timer to start counting once a partner is assigned.
            tripRequest.setExpiredTime(LocalDateTime.now().plusMinutes(Long.parseLong(tripRequestDueForExpireTimeInMiliseconds)/(60*1000)));
            tripRequest.setUpdatedDate(LocalDateTime.now());// update the time to be used for tracking during expiration
            tripRequest.setAssignedDate(LocalDateTime.now());
        }
        if (request.getDriverUserId() != null) {

            Driver driver = driverRepository.findByUserId(request.getDriverUserId());

            User user = userRepository.getOne(driver.getUserId());
            tripRequest.setDriverId(driver.getId());
            tripRequest.setDriverUserId(driver.getUserId());
            tripRequest.setDriverName(user.getLastName() + " " + user.getFirstName());
            //Has the driver accepted the trip?
            if (request.getDriverStatus()!=null && request.getDriverStatus().equalsIgnoreCase("accepted")){
                //Then update the driver's status accordingly
                tripRequest.setDriverStatus(tripRequest.getStatus());
            }
            // sets up timer for this driver and update the date for tracking during expiration
            tripRequest.setExpiredTime(LocalDateTime.now().plusMinutes(Long.parseLong(tripRequestDueForExpireTimeInMiliseconds)/(60*1000)));
            tripRequest.setUpdatedDate(LocalDateTime.now());
            tripRequest.setAssignedDate(LocalDateTime.now());
        }
        if (request.getDriverAssistantUserId() != null) {

            Driver driver2 = driverRepository.findByUserId(request.getDriverAssistantUserId());

            User user2 = userRepository.getOne(driver2.getUserId());
            tripRequest.setDriverAssistantId(driver2.getId());
            tripRequest.setDriverAssistantUserId(driver2.getUserId());
            tripRequest.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());
            //Has the driver accepted the trip?
            if (!Objects.isNull(request.getDriverStatus()) && request.getDriverStatus().equalsIgnoreCase("accepted")){
                //Then update the driver's status accordingly
                tripRequest.setDriverStatus(tripRequest.getStatus());
            }
            // sets up timer for this assistant driver and update the date for tracking during expiration
            tripRequest.setExpiredTime(LocalDateTime.now().plusMinutes(Long.parseLong(tripRequestDueForExpireTimeInMiliseconds)/(60*1000)));
            tripRequest.setUpdatedDate(LocalDateTime.now());
            tripRequest.setAssignedDate(LocalDateTime.now());
        }
        tripRequest.setUpdatedBy(userCurrent.getId());
        tripRequestRepository.save(tripRequest);
        log.debug("tripRequest record updated - {}"+ tripRequest);
        TripResponseDto tripResponseDto = mapper.map(tripRequest, TripResponseDto.class);

        if(request.getPartnerId() != null) {
            Partner partner = partnerRepository.findPartnerById(request.getPartnerId());
            if (partner == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Partner Id");
            }
            tripResponseDto.setPartnerName(partner.getName());
        }

        if (request.getPartnerAssetId() != null) {
            PartnerAsset partnerAsset = partnerAssetRepository.findPartnerAssetById(request.getPartnerAssetId());
            if (partnerAsset == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid PartnerAsset Id");
            };
            tripResponseDto.setPartnerAssetName(partnerAsset.getName());
        }

        return tripResponseDto;
    }

    public void loadTripsQualifiedForExpiration() {
        this.tripsDueForExpiration = tripRequestRepository.findByPartnerIdNotNullAndExpiredTimeNotNullAndStatus("pending");
        tripsDueForExpiration.addAll(tripRequestRepository.findAllTripsNotYetAcceptedByADriver("accepted","pending"));
    }
    public List<TripRequest> getTripsDueForExpiration(){
        return  this.tripsDueForExpiration;
    }

    /**
     *  @Description: A background job that expires trips not yet accepted by eithe driver or partner.
     *  @Date: 26/04/2022
     * @Author: Okonkwo Afam
     */
    public void expireUnAcceptedTrips() {
        List<TripRequest> expiredTripsList = this.getTripsDueForExpiration();
                //tripRequestRepository.findByPartnerIdNotNullAndExpiredTimeNotNullAndStatus("pending");
        log.info("Current partner expired trips={}",expiredTripsList);
        Long configExpirationTimeInMinutes = Long.parseLong(tripRequestDueForExpireTimeInMiliseconds)/(60*1000); // get the time config time for expiration
        configExpirationTimeInMinutes+=1; // Add one to explicitly ensure that the result is before the assignedTime
        for (Iterator<TripRequest> tripRequestIterator = expiredTripsList.iterator(); tripRequestIterator.hasNext();){
            TripRequest tripRequest = tripRequestIterator.next();
            TripRequestResponse tripRequestResponse = tripRequestResponseRepository.findByTripRequestIdAndPartnerId(tripRequest.getId(),tripRequest.getPartnerId());
            if (tripRequestResponse==null){
                    log.info("This trip with id "+tripRequest.getId()+" doesn't have any response yet on the TripRequestResponse table. Hence the expiration scheduler will be throwing error");
            }
            //Is it up to atleast the expired minutes since the trip was assigned?
            if (tripRequest.getExpiredTime().minusMinutes(configExpirationTimeInMinutes).isBefore(tripRequest.getAssignedDate())){
                if (tripRequest.getPartnerId() !=null && tripRequest.getStatus().equalsIgnoreCase("pending")){
                    tripRequestResponse.setStatus("expired");
                    tripRequestResponseRepository.save(tripRequestResponse);
                    tripRequest.setStatus("pending");
                    tripRequest.setExpiredTime(null);
                    tripRequest.setPartnerId(null);
                    tripRequest.setPartnerAssetId(null);
                    log.info("partnerId {}", tripRequest.getPartnerId());
                    tripRequestRepository.save(tripRequest);
                    log.info("Successfully expired UnAccepted trips assigned to partner with id {}",tripRequest.getPartnerId());
                    //remove from the pool while avoiding ConcurrentModification exception
                    tripRequestIterator.remove(); // Remove the expired trip from the pool of trips due for expiration.
                }
            }
        }
        //Retrieve and get all those trips accepted by a partner but not yet accepted by a driver
        expiredTripsList = this.getTripsDueForExpiration();
                //tripRequestRepository.findAllTripsNotYetAcceptedByADriver("accepted","pending");
        log.info("Current driver expired trips={}",expiredTripsList);
        for (Iterator<TripRequest> tripRequestIterator = expiredTripsList.iterator(); tripRequestIterator.hasNext();) {
            TripRequest tripRequest = tripRequestIterator.next();
            TripRequestResponse tripRequestResponse = tripRequestResponseRepository.findByTripRequestIdAndPartnerId(tripRequest.getId(),tripRequest.getPartnerId());
            if (tripRequestResponse==null){
                log.info("This trip with id "+tripRequest.getId()+" doesn't have any response yet on the TripRequestResponse table. Hence the expiration scheduler will be throwing error");
            }
            //Is this trip due for expiration?
            if(tripRequest.getExpiredTime().minusMinutes(configExpirationTimeInMinutes).isBefore(tripRequest.getAssignedDate())){
                if (tripRequest.getPartnerId() != null && tripRequest.getStatus().equalsIgnoreCase("accepted") && (tripRequest.getDriverId() !=null || tripRequest.getDriverAssistantId()!=null) && tripRequest.getDriverStatus().equalsIgnoreCase("pending")){
                    //get the driver instance to be used later for emails and other communications
                    Driver driver = null;
                    Driver driverAssitant = null;
                    User  driverUser = null;
                    if (tripRequest.getDriverId()!=null){

                        driver = driverRepository.findDriverById(tripRequest.getDriverId());
                        if (tripRequest.getDriverId() != null){
                            tripRequest.setDriverId(null);
                            tripRequest.setDriverStatus("pending");
                            driverUser = userRepository.findById(driver.getUserId()).orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"Driver userId not found!"));
                        }
                    }
                    if (tripRequest.getDriverAssistantId()!=null){
                        tripRequest.setDriverAssistantId(null);
                        driverAssitant = driverRepository.findDriverById(tripRequest.getDriverAssistantId());
                    }
                    log.info("Successfully expired UnAccepted trips assigned to a driver with id {}",(tripRequest.getDriverId()!=null ? tripRequest.getDriverId() : tripRequest.getDriverAssistantId()));
                    tripRequestRepository.save(tripRequest);
                    //remove from the pool while avoiding ConcurrentModification exception
                    tripRequestIterator.remove();
                    if (driverUser!=null)
                        this.dispatchTripExpiryNotificationsToPartner(tripRequest.getPartnerId(),"Your Driver "+driverUser.getFirstName()+" failed to accept the assigned trip with reference number "+tripRequest.getReferenceNo()+" before it expired");
                    if (driverAssitant!=null){
                        User driverAssitantUser = userRepository.findById(driverAssitant.getUserId()).orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"Assitant DriverUserId not found"));
                        this.dispatchTripExpiryNotificationsToPartner(tripRequest.getPartnerId(),"Your Assitant Driver "+driverAssitantUser.getFirstName()+" failed to accept the assigned trip with reference number "+tripRequest.getReferenceNo()+" before it expired");
                    }
                }
            }
        }
    }

    private void dispatchTripExpiryNotificationsToPartner(long partnerId, String message){

        Partner partner = partnerRepository.findPartnerById(partnerId);
        if(partner != null){
            User user = userRepository.findById(partner.getUserId())
                    .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The userId of this driver is not found!"));

            generalNotificationService.dispatchNotificationsToUser(user,user.getPhone(), message);

        }
        else
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The partnerId needed to send trip expiry notification doesn't exist");
        log.info("Trip Expiry notification successfully sent to partner {} ",partner.getName());
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

        if (tripResponseDto.getPartnerId() != null) {
            Partner partner = partnerRepository.findPartnerById(tripResponseDto.getPartnerId());
            if (partner == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Partner Id");
            }
            tripResponseDto.setPartnerName(partner.getName());
        }

        if (tripResponseDto.getPartnerAssetId() != null) {
            PartnerAsset partnerAsset = partnerAssetRepository.findPartnerAssetById(tripResponseDto.getPartnerAssetId());
            if (partnerAsset == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid PartnerAsset Id");
            }
            tripResponseDto.setPartnerAssetName(partnerAsset.getName());
        }

        if (tripResponseDto.getDriverId() != null) {
            Driver driver = driverRepository.findDriverById(tripResponseDto.getDriverId());

            User user = userRepository.getOne(driver.getUserId());
            tripResponseDto.setDriverName(user.getLastName() + " " + user.getFirstName());
            tripResponseDto.setDriverPhone(user.getPhone());
        }

        if (tripResponseDto.getDriverAssistantId() != null) {
            Driver driver2 = driverRepository.findDriverById(tripResponseDto.getDriverAssistantId());

            User user2 = userRepository.getOne(driver2.getUserId());
            tripResponseDto.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());
            tripResponseDto.setDriverAssistantPhone(user2.getPhone());
        }
        tripResponseDto.setCurrentSystemTime(LocalDateTime.now()); // Needed for frontEnd calculation of trip's expiredTime
        return tripResponseDto;
    }


    public Page<TripRequest> findAll(Long partnerId, String status, String referenceNo, Long driverUserId, Long driverAssistantUserId,
                                     Long wareHouseId, String wareHouseAddress, Long partnerAssetId, Boolean unassignedPartner, String deliveryStatus, Boolean unassignedDriver, PageRequest pageRequest ){

        Long driverId = null;
        Long driverAssistantId = null;

        if(driverUserId != null) {
            Driver driver = driverRepository.findByUserId(driverUserId);
            driverId = driver.getId();

        }

        if(driverAssistantUserId != null) {
            Driver driver2 = driverRepository.findByUserId(driverAssistantUserId);
            driverAssistantId = driver2.getId();

        }

        Page<TripRequest> tripRequests = tripRequestRepository.findTripRequest(partnerId, status, referenceNo, driverId, driverAssistantId,
                                                                                wareHouseId, wareHouseAddress, partnerAssetId, unassignedPartner, deliveryStatus, unassignedDriver, pageRequest);
        if(tripRequests == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        tripRequests.getContent().forEach(request ->{

            if (request.getPartnerId() != null && !request.getPartnerId().equals(0)) {
                Partner partner = partnerRepository.findPartnerById(request.getPartnerId());
                if (partner == null) {
                    throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Partner Id");
                }
                if (partner.getName() != null || !partner.getName().isEmpty()){
                    request.setPartnerName(partner.getName());
                }
            }

            if (request.getPartnerAssetId() != null && !request.getPartnerAssetId().equals(0)) {
                PartnerAsset partnerAsset = partnerAssetRepository.findPartnerAssetById(request.getPartnerAssetId());
                if (partnerAsset == null) {
                    throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid PartnerAsset Id");
                }

                if(partnerAsset.getName() != null || !partnerAsset.getName().isEmpty()){
                    request.setPartnerAssetName(partnerAsset.getName());
                    request.setPartnerAssetTypeName(partnerAsset.getAssetTypeName());
                }
            }

            if (request.getDriverId() != null && !request.getDriverId().equals(0)) {
                Driver driver1 = driverRepository.findDriverById(request.getDriverId());
                User user = userRepository.getOne(driver1.getUserId());
                if(user.getFirstName() != null || user.getLastName() != null || !(user.getFirstName().isEmpty() || user.getLastName().isEmpty())){
                    request.setDriverName(user.getLastName() + " " + user.getFirstName());
                    request.setDriverPhone(user.getPhone());
                }
            }

            if (request.getDriverAssistantId() != null && !request.getDriverAssistantId().equals(0)) {
                Driver driver2 = driverRepository.findDriverById(request.getDriverAssistantId());
                User user2 = userRepository.getOne(driver2.getUserId());
                if(user2.getFirstName() != null || user2.getLastName() != null || !(user2.getFirstName().isEmpty() || user2.getLastName().isEmpty())){
                    request.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());
                    request.setDriverAssistantPhone(user2.getPhone());
                }
            }

            request.setDropOffCount(getDropOff(request.getId()));

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
        tripRequests.forEach(request -> {
            Partner partner = partnerRepository.findPartnerById(request.getPartnerId());
            if (partner != null) {
                request.setPartnerName(partner.getName());
            }
            PartnerAsset partnerAsset = partnerAssetRepository.findPartnerAssetById(request.getPartnerAssetId());
            if (partnerAsset != null) {
                request.setPartnerAssetName(partnerAsset.getName());
            }
            ;
            Driver driver = driverRepository.findDriverById(request.getDriverId());
            if (driver != null) {
                User user = userRepository.getOne(driver.getUserId());
                request.setDriverName(user.getLastName() + " " + user.getFirstName());
                request.setDriverPhone(user.getPhone());
            }
            Driver driver2 = driverRepository.findDriverById(request.getDriverAssistantId());
            if (driver2 != null) {
                User user2 = userRepository.getOne(driver2.getUserId());
                request.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());
                request.setDriverAssistantPhone(user2.getPhone());
            }
            request.setDropOffCount(getDropOff(request.getId()));
        });
        return tripRequests;

    }

    public List<TripItem> getAllTripItems(Long tripRequestId){
        List<TripItem> tripItems = tripItemRepository.findByTripRequestId(tripRequestId);

        return tripItems;

    }

    public List<DropOff> getAllDropOffs(Long tripRequestId){
        List<DropOff> dropOffs = dropOffRepository.findByTripRequestId(tripRequestId);

        for (DropOff dropOff : dropOffs) {

            Invoice invoice = invoiceRepository.getOne(dropOff.getInvoiceId());
            dropOff.setCustomerName(invoice.getCustomerName());
            dropOff.setDeliveryAddress(invoice.getDeliveryAddress());
            dropOff.setCustomerPhone(invoice.getCustomerPhone());


            if (dropOff.getPaymentStatus() != null && dropOff.getPaymentStatus() == PaymentStatus.PayOnDelivery) {
                List<DropOffItem> dropOffItems = dropOffItemRepository.findByDropOffId(dropOff.getId());
                dropOff.setTotalAmount(getTotalAmount(dropOffItems));
            }

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

            InvoiceItem invoiceItem = invoiceItemRepository.getOne(dropOffItem.getInvoiceItemId());
            Invoice invoice = invoiceRepository.getOne(invoiceItem.getInvoiceId());
            dropOffItem.setCustomerName(invoice.getCustomerName());
            dropOffItem.setCustomerPhone(invoice.getCustomerPhone());
            dropOffItem.setInvoiceItemName(invoiceItem.getProductName());
            dropOffItem.setThirdPartyProductId(invoiceItem.getThirdPartyProductId());
            dropOffItem.setQty(invoiceItem.getQty());
            dropOffItem.setInvoiceId(invoiceItem.getInvoiceId());
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

    private BigDecimal getTotalAmount(List<DropOffItem> dropOffItems) {
        return ((BigDecimal)dropOffItems.stream().filter(Objects::nonNull).map(DropOffItem::getAmountCollected).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private Number getTotalItemsPickedUp(List<TripItem> tripItems) {
        return ((Number)tripItems.stream().filter(Objects::nonNull).map(TripItem::getQtyPickedUp).reduce(Integer.valueOf(0), Integer::sum));
    }




    public void shipmentTripRequest(ShipmentTripRequest request){

        TripRequest tripRequest = TripRequest.builder()
                .partnerId(request.getLogisticPartnerId())
                .deliveryDate(request.getDeliveryDate())
                .wareHouseId(request.getWarehouseId())
                .referenceNo(String.valueOf(request.getId()))
                .contactPhone(request.getPhoneNumber())
                .partnerAssetId(request.getAssestId())
                .earnings(request.getTotalAmount())
                .status(request.getStatus())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .driverAssistantId(0l)
                .driverId(0l)
                .weight(0)
                .barCode(validations.generateCode(String.valueOf(request.getId())))
                .qrCode(validations.generateCode(String.valueOf(request.getId())))
                .build();
        TripRequest trip = tripRequestRepository.findByReferenceNo(tripRequest.getReferenceNo());
        if(trip == null) {
            tripRequestRepository.save(tripRequest);
        }


    }

    public List<InvoiceItem> updateVerificationStatus(InvoiceItemVerificationDto request) {
        validations.validateInvoiceItemVerificationStatus(request);
        Invoice invoice = invoiceRepository.findByReferenceNo(request.getInvoiceReference());
        List<InvoiceItem> invoiceItems = invoiceItemRepository.findByInvoiceId(invoice.getId());
        log.info("Current size of invoiceItems=={}",invoiceItems.size());
        for (InvoiceItem invoiceItem: invoiceItems){
            invoiceItem.setVerificationStatus(request.getVerificationStatus());
            invoiceItemRepository.save(invoiceItem);
        }
        return invoiceItems;
    }

}
