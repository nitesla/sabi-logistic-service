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
import com.sabi.logistics.core.dto.response.*;
import com.sabi.logistics.core.enums.PaymentStatus;
import com.sabi.logistics.core.enums.SlaName;
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
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@SuppressWarnings("All")
@Service
@Slf4j
@EnableAsync
public class TripRequestService {

    @Value("${trip-request-due-for-expiry-db-load-time}")
    private String tripRequestDueForExpireTimeInMiliseconds;
    private Long SLA_TIME_ACCEPT_TRIP_REQUEST;
    private Long SLA_TIME_START_TRIP;
    private Long SLA_TIME_ASSIGN_TRIP_TO_DRIVER;
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
    private InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private TripRequestResponseService tripRequestResponseService;

    @Autowired
    private WarehouseRepository warehouseRepository;

    private final DropOffService dropOffService;

    @Autowired
    private DropOffItemService dropOffItemService;

    @Autowired
    private InvoiceItemService invoiceItemService;

    @Autowired
    private DashboardSummaryRepository dashboardSummaryRepository;

    private List<TripRequest> tripsDueForExpiration;

    private final GeneralNotificationService generalNotificationService;

    private final SLAService slaService;

    private final   SLANotifierRepository slaNotifierRepository;
    private  SLAResponseDto ACCEPT_TRIP_REQUEST_SLA;
    private  SLAResponseDto START_TRIP_SLA;
    private  SLAResponseDto ASSIGN_TRIP_TO_DRIVER_SLA;
    private List<String> driverPartnerStatuses;


    public TripRequestService(TripRequestRepository tripRequestRepository, ModelMapper mapper, NotificationService notificationService, WhatsAppService whatsAppService, DropOffService dropOffService, GeneralNotificationService generalNotificationService, SLAService slaService, SLANotifierRepository slaNotifierRepository) {
        this.tripRequestRepository = tripRequestRepository;
        this.mapper = mapper;
        this.dropOffService = dropOffService;
        this.generalNotificationService = generalNotificationService;
        this.slaService = slaService;
        this.slaNotifierRepository = slaNotifierRepository;
        this.tripsDueForExpiration = new ArrayList<>();
        this.driverPartnerStatuses = Arrays.asList("pending","accepted");
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
            this.ACCEPT_TRIP_REQUEST_SLA = slaService.findSLAByName(SlaName.ACCEPT_TRIP_REQUEST);
            tripRequest.setExpiredTime(LocalDateTime.now().plusMinutes(ACCEPT_TRIP_REQUEST_SLA.getSlaDuration())); // sets the time SLA for accepting trips after creation
            tripRequest.setSLAId(ACCEPT_TRIP_REQUEST_SLA.getId());
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
            this.ACCEPT_TRIP_REQUEST_SLA = slaService.findSLAByName(SlaName.ACCEPT_TRIP_REQUEST);
            tripRequest.setExpiredTime(LocalDateTime.now().plusMinutes(ACCEPT_TRIP_REQUEST_SLA.getSlaDuration()));
            tripRequest.setSLAId(ACCEPT_TRIP_REQUEST_SLA.getId());
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
                //Sets up a timer for assigning trips to a driver
                this.ASSIGN_TRIP_TO_DRIVER_SLA = slaService.findSLAByName(SlaName.ASSIGN_TRIP_TO_DRIVER);
                tripRequest.setExpiredTime(LocalDateTime.now().plusMinutes(ASSIGN_TRIP_TO_DRIVER_SLA.getSlaDuration()));
                tripRequest.setSLAId(ASSIGN_TRIP_TO_DRIVER_SLA.getId());
                tripRequest.setAssignedDate(LocalDateTime.now());
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
            this.ACCEPT_TRIP_REQUEST_SLA = slaService.findSLAByName(SlaName.ACCEPT_TRIP_REQUEST);
            tripRequest.setExpiredTime(LocalDateTime.now().plusMinutes(ACCEPT_TRIP_REQUEST_SLA.getSlaDuration())); // sets the time SLA for accepting trips after creation
            tripRequest.setSLAId(ACCEPT_TRIP_REQUEST_SLA.getId());
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
                //Sets driver SLA time to start a trip
                this.START_TRIP_SLA = slaService.findSLAByName(SlaName.START_TRIP);
                tripRequest.setExpiredTime(LocalDateTime.now().plusMinutes(START_TRIP_SLA.getSlaDuration()));
                tripRequest.setSLAId(START_TRIP_SLA.getId());
                tripRequest.setAssignedDate(LocalDateTime.now());
                //get DropOffs of a Driver, generate DeliveryCode & send deliveryCode and update
                List<DropOff> driversDropOffs = dropOffService.getDropOffsByTripRequestId(tripRequest.getId());
                dropOffService.generateDeliveryCodeUpdateDropOffAndSend(driversDropOffs);
            }
            if (request.getDriverStatus()!=null && request.getDriverStatus().equalsIgnoreCase("pending")){

                // sets up timer for this driver when the status is 'pending' - that's yet to accept the assigned trip
                this.ACCEPT_TRIP_REQUEST_SLA = slaService.findSLAByName(SlaName.ACCEPT_TRIP_REQUEST);
                tripRequest.setExpiredTime(LocalDateTime.now().plusMinutes(ACCEPT_TRIP_REQUEST_SLA.getSlaDuration())); // sets the time SLA for accepting trips after creation
                tripRequest.setSLAId(ACCEPT_TRIP_REQUEST_SLA.getId());
                tripRequest.setUpdatedDate(LocalDateTime.now());
                tripRequest.setAssignedDate(LocalDateTime.now());
            }

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
                //Sets driver SLA time to start a trip
                this.START_TRIP_SLA = slaService.findSLAByName(SlaName.START_TRIP);
                tripRequest.setExpiredTime(LocalDateTime.now().plusMinutes(START_TRIP_SLA.getSlaDuration()));
                tripRequest.setSLAId(START_TRIP_SLA.getId());
                tripRequest.setAssignedDate(LocalDateTime.now());
                //get DropOffs of a Driver, generate DeliveryCode & send deliveryCode and update
                List<DropOff> driversDropOffs = dropOffService.getDropOffsByTripRequestId(tripRequest.getId());
                dropOffService.generateDeliveryCodeUpdateDropOffAndSend(driversDropOffs);
            }
            if (request.getDriverStatus()!=null && request.getDriverStatus().equalsIgnoreCase("pending")) {
                // sets up timer for this driver when the status is 'pending' - that's yet to accept the assigned trip
                this.ACCEPT_TRIP_REQUEST_SLA = slaService.findSLAByName(SlaName.ACCEPT_TRIP_REQUEST);
                tripRequest.setExpiredTime(LocalDateTime.now().plusMinutes(ACCEPT_TRIP_REQUEST_SLA.getSlaDuration())); // sets the time SLA for accepting trips after creation
                tripRequest.setSLAId(ACCEPT_TRIP_REQUEST_SLA.getId());
                tripRequest.setAssignedDate(LocalDateTime.now());
                tripRequest.setUpdatedDate(LocalDateTime.now());
            }
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
        this.tripsDueForExpiration = tripRequestRepository.findAllTripsDueForExpirations(driverPartnerStatuses);

    }
    public List<TripRequest> getTripsDueForExpiration(){
        return  this.tripsDueForExpiration;
    }

    /**
     *  @Description: A background job that expires PARTNER/DRIVER ACCEPTED TRIPS not yet accepted by eithe driver or partner.
     *  @Date: 26/04/2022
     * @Author: Okonkwo Afam
     */
    public void expireUnAcceptedPartnersDriversTrips() {
        List<TripRequest> expiredTripsList = this.getTripsDueForExpiration();
                //tripRequestRepository.findByPartnerIdNotNullAndExpiredTimeNotNullAndStatus("pending");
        log.info("Current partner's unaccepted trips to be expired={}",expiredTripsList);
        this.ACCEPT_TRIP_REQUEST_SLA = slaService.findSLAByName(SlaName.ACCEPT_TRIP_REQUEST);
        for (Iterator<TripRequest> tripRequestIterator = expiredTripsList.iterator(); tripRequestIterator.hasNext();){
            TripRequest tripRequest = tripRequestIterator.next();
            TripRequestResponse tripRequestResponse = tripRequestResponseRepository.findByTripRequestIdAndPartnerId(tripRequest.getId(),tripRequest.getPartnerId());
            if (tripRequestResponse==null){
                    log.info("This trip with id "+tripRequest.getId()+" doesn't have any response yet on the TripRequestResponse table. Hence the expiration scheduler will be throwing error");
            }
            //Is it up to atleast the expired minutes since the trip was assigned?
            if (tripRequest.getExpiredTime().isBefore(LocalDateTime.now())){
                if (tripRequest.getPartnerId() !=null && tripRequest.getStatus().equalsIgnoreCase("pending")){
                    Partner partner = partnerRepository.findPartnerById(tripRequest.getPartnerId());
                    User partnerUser = userRepository.findById(partner.getUserId()).orElseThrow(()->new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"The requested partner Id assigned to this expired trip could not be found"));
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
                    log.info("Preparing to send email to a partner. Email::"+partnerUser.getEmail());
                    if (partnerUser!=null)
                        pushExpiredTripNotificationsToPartnerAndDriver(tripRequest, partnerUser,null, "Your Accepted Trip with  reference number: "+tripRequest.getReferenceNo()+" has expired since you failed to accept the trip  within the agreed time", SlaName.ACCEPT_TRIP_REQUEST,null);
                }
            }
        }
        //Retrieve and get all those trips accepted by a partner and assigned to a driver but not yet accepted by a driver
        expiredTripsList = this.getTripsDueForExpiration();
        log.info("Current driver's unaccepted trips to be expired={}",expiredTripsList);
        for (Iterator<TripRequest> tripRequestIterator = expiredTripsList.iterator(); tripRequestIterator.hasNext();) {
            TripRequest tripRequest = tripRequestIterator.next();
            TripRequestResponse tripRequestResponse = tripRequestResponseRepository.findByTripRequestIdAndPartnerId(tripRequest.getId(),tripRequest.getPartnerId());
            if (tripRequestResponse==null){
                log.info("This trip with id "+tripRequest.getId()+" doesn't have any response yet on the TripRequestResponse table. Hence the expiration scheduler will be throwing error");
            }
            //Is this trip due for expiration?
            if(tripRequest.getExpiredTime().isBefore(LocalDateTime.now())){
                if (tripRequest.getPartnerId() != null && tripRequest.getStatus().equalsIgnoreCase("accepted") && (tripRequest.getDriverId() !=null || tripRequest.getDriverAssistantId()!=null) && tripRequest.getDriverStatus()!= null && tripRequest.getDriverStatus().equalsIgnoreCase("pending")){
                    //get the driver instance to be used later for emails and other communications
                    Driver driver = null;
                    Driver driverAssitant = null;
                    Partner partner = partnerRepository.findPartnerById(tripRequest.getPartnerId());
                    User partnerUser = userRepository.findById(partner.getUserId()).orElseThrow(()->new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"The requested partner Id assigned to this expired trip could not be found"));
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
                    if (driverUser!=null) {
                        pushExpiredTripNotificationsToPartnerAndDriver(tripRequest, partnerUser,null,"Your driver "+driverUser.getFirstName()+"  failed to ACCEPT the trip with reference no "+tripRequest.getReferenceNo()+" before it expired", SlaName.START_TRIP,"Your Driver ");
                        pushExpiredTripNotificationsToPartnerAndDriver(tripRequest, null, driverUser,null, SlaName.ACCEPT_TRIP_REQUEST,"Your Driver ");
                    }
                    if (driverAssitant!=null){
                        pushExpiredTripNotificationsToPartnerAndDriver(tripRequest, partnerUser,null,"Your assitant driver "+driverUser.getFirstName()+"  failed to ACCEPT the trip with reference no "+tripRequest.getReferenceNo()+" before it expired", SlaName.START_TRIP,"Your Driver ");
                        User driverAssitantUser = userRepository.findById(driverAssitant.getUserId()).orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"Assitant DriverUserId not found"));
                        pushExpiredTripNotificationsToPartnerAndDriver(tripRequest, null, driverAssitantUser,null, SlaName.ACCEPT_TRIP_REQUEST, "Your Assitant Driver ");
                    }
                }
            }
        }
    }


    /**
     *  @Description: A background job that expires PARTNER UNASSIGNED  TRIPS not yet accepted by eithe driver or partner.
     *  @Date: 24/06/2022
     * @Author: Okonkwo Afam
     */
    public void expireUnAssignedPartnerTrips() {
        List<TripRequest> expiredTripsList = this.getTripsDueForExpiration();
        log.info("Current partner's unassigned trips to be expired={}",expiredTripsList);
        this.ASSIGN_TRIP_TO_DRIVER_SLA = slaService.findSLAByName(SlaName.ASSIGN_TRIP_TO_DRIVER);
        for (Iterator<TripRequest> tripRequestIterator = expiredTripsList.iterator(); tripRequestIterator.hasNext();){
            TripRequest tripRequest = tripRequestIterator.next();
            TripRequestResponse tripRequestResponse = tripRequestResponseRepository.findByTripRequestIdAndPartnerId(tripRequest.getId(),tripRequest.getPartnerId());
            if (tripRequestResponse==null){
                log.info("This trip with id "+tripRequest.getId()+" doesn't have any response yet on the TripRequestResponse table. Hence the expiration scheduler will be throwing error");
            }
            //Has the trip expired?
            if (tripRequest.getExpiredTime().isBefore(LocalDateTime.now())){
                if (tripRequest.getPartnerId() !=null && tripRequest.getStatus().equalsIgnoreCase("accepted")  &&  (tripRequest.getDriverId() == null || tripRequest.getDriverAssistantId() == null)){
                    Partner partner = partnerRepository.findPartnerById(tripRequest.getPartnerId());
                    User partnerUser = userRepository.findById(partner.getUserId()).orElseThrow(()->new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"The requested partner Id assigned to this expired trip could not be found"));
                    tripRequestResponse.setStatus("expired");
                    tripRequestResponseRepository.save(tripRequestResponse);
                    tripRequest.setStatus("pending");
                    tripRequest.setExpiredTime(null);
                    tripRequest.setPartnerId(null);
                    tripRequest.setPartnerAssetId(null);
                    tripRequestRepository.save(tripRequest);
                    log.info("Successfully expired Driver-UnAssigned trips assigned to partner with UserId {}",partnerUser.getId());
                    //remove from the pool while avoiding ConcurrentModification exception
                    tripRequestIterator.remove(); // Remove the expired trip from the pool of trips due for expiration.
                    log.info("Preparing to send email to a partner. Email::"+partnerUser.getEmail());
                    if (partnerUser!=null)
                        pushExpiredTripNotificationsToPartnerAndDriver(tripRequest, partnerUser, null,"Your Accepted Trip with reference number "+tripRequest.getReferenceNo()+" has expired since you failed to assign the trip to any of your driver  within the agreed time", SlaName.ASSIGN_TRIP_TO_DRIVER,null);
                }
            }
        }

    }

    /**
     *  @Description: A background job that expires DRIVER'S NON-STARTED TRIPS not yet accepted by eithe driver or partner.
     *  @Date: 24/06/2022
     * @Author: Okonkwo Afam
     */
    public void expireNonStartedDriverTrips() {
        List<TripRequest> expiredTripsList = this.getTripsDueForExpiration();
        //tripRequestRepository.findByPartnerIdNotNullAndExpiredTimeNotNullAndStatus("pending");
        log.info("Current driver's non-started trips to be expired={}",expiredTripsList);
        //get the time config time for expiration and add 1
        this.START_TRIP_SLA = slaService.findSLAByName(SlaName.START_TRIP);
        Long SLA_TIME_START_TRIP = START_TRIP_SLA.getSlaDuration();
        for (Iterator<TripRequest> tripRequestIterator = expiredTripsList.iterator(); tripRequestIterator.hasNext();) {
            TripRequest tripRequest = tripRequestIterator.next();
            TripRequestResponse tripRequestResponse = tripRequestResponseRepository.findByTripRequestIdAndPartnerId(tripRequest.getId(),tripRequest.getPartnerId());
            if (tripRequestResponse==null){
                log.info("This trip with id "+tripRequest.getId()+" doesn't have any response yet on the TripRequestResponse table. Hence the expiration scheduler will be throwing error");
            }
            //Is this trip due for expiration?
            if(tripRequest.getExpiredTime().isBefore(LocalDateTime.now())){
                if (tripRequest.getPartnerId() != null && tripRequest.getStatus().equalsIgnoreCase("accepted") && (tripRequest.getDriverId() !=null || tripRequest.getDriverAssistantId()!=null) && tripRequest.getDriverStatus()!= null && tripRequest.getDriverStatus().equalsIgnoreCase("accepted")){
                    //get the driver instance to be used later for emails and other communications
                    Driver driver = null;
                    Driver driverAssitant = null;
                    Partner partner = partnerRepository.findPartnerById(tripRequest.getPartnerId());
                    User partnerUser = userRepository.findById(partner.getUserId()).orElseThrow(()->new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"The requested partner Id assigned to this expired trip could not be found"));
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
                    tripRequestRepository.save(tripRequest);
                    log.info("Successfully expired Non-Started trips assigned to a driver with id {}",driver.getId());
                    //remove from the pool while avoiding ConcurrentModification exception
                    tripRequestIterator.remove();
                    if (driverUser!=null) {
                        log.info("Pushing Message to Partner after trip expiration... Email=="+partnerUser.getEmail());
                        pushExpiredTripNotificationsToPartnerAndDriver(tripRequest, partnerUser,null,"Your driver "+driverUser.getFirstName()+"  failed to start the trip with reference no "+tripRequest.getReferenceNo()+" before it expired", SlaName.START_TRIP,"Your Driver ");
                        log.info("Pushing Message to Driver after trip expiration...");
                        pushExpiredTripNotificationsToPartnerAndDriver(tripRequest, null, driverUser,null, SlaName.START_TRIP,"Your Driver ");
                        log.info("ALL NOTIFICATIONS SUCCESSFULLY SENT AFTER EXPIRATION");
                    }
                    if (driverAssitant!=null){
                        User driverAssitantUser = userRepository.findById(driverAssitant.getUserId()).orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"Assitant DriverUserId not found"));
                        log.info("Pushing Message to Partner after trip expiration...");
                        pushExpiredTripNotificationsToPartnerAndDriver(tripRequest, partnerUser,null,"Your assitant driver "+driverAssitantUser.getFirstName()+"  failed to start the trip with reference no "+tripRequest.getReferenceNo()+" before it expired", SlaName.START_TRIP,"Your Driver ");
                        log.info("Pushing Message to Assitant Driver after trip expiration...");
                        pushExpiredTripNotificationsToPartnerAndDriver(tripRequest, null, driverAssitantUser,null, SlaName.START_TRIP, "Your Assitant Driver ");
                    }
                }
            }
        }
    }

    /**
     *  @Description: A background job that triggers NOTIFICATIONS TO PARTNERS FOR TRIPS about to expire.
     *  @Date: 24/06/2022
     * @Author: Okonkwo Afam
     */
    public void triggerNotificationsWarningOfNearBreachesTrips() {
        List<TripRequest> expiredTripsList = tripRequestRepository.findAllTripsDueForExpirations(driverPartnerStatuses);

        log.info("Current trips to trigger notifications for near breaches={}",expiredTripsList);
        for (Iterator<TripRequest> tripRequestIterator = expiredTripsList.iterator(); tripRequestIterator.hasNext();){
            TripRequest tripRequest = tripRequestIterator.next();
            //Is this trip nearing the expiration-time- before it is breached?
            if (tripRequest.getSLAId() == null)
                continue;
                //throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Can't send notifications for a trip that doesn't have an SLA id. TripRequestId ="+tripRequest.getId());
            SLAResponseDto sla = slaService.findSingleSLA(tripRequest.getSLAId());
            if (sla.getTriggerDuration() !=null && tripRequest.getExpiredTime().isAfter(LocalDateTime.now()) && ((tripRequest.getExpiredTime().getMinute()-LocalDateTime.now().getMinute())<=sla.getTriggerDuration())){
                    log.info("Preparing to send near breaches notifications trigger...");
                    Partner partner = partnerRepository.findById(tripRequest.getPartnerId())
                            .orElseThrow(()->new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"PartnerId of the triRequest doesn't exist and thus can't be found"));
                    User partnerUser = userRepository.findById(partner.getUserId())
                            .orElseThrow(()-> new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"The UserId of the partner that accepted the tripRequest- with reference no: "+tripRequest.getReferenceNo()+"- does not exist"));
                    //remove from the pool while avoiding ConcurrentModification exception
                    String breachWarningMessageToSend = "";
                    switch (sla.getSlaName()){
                        case START_TRIP:
                            breachWarningMessageToSend = "Your trip with reference number "+tripRequest.getReferenceNo()+" which was assigned to you and you accepted will soon expire in "+(tripRequest.getExpiredTime().getMinute()-LocalDateTime.now().getMinute())+" mins . You must START IT within the agreed time";
                            break;
                        case ACCEPT_TRIP_REQUEST:
                            breachWarningMessageToSend = "Your trip with reference number "+tripRequest.getReferenceNo()+" which was assigned to you will soon expire in "+(tripRequest.getExpiredTime().getMinute()-LocalDateTime.now().getMinute())+" mins . You must ACCEPT IT within the agreed time";
                            break;
                        case ASSIGN_TRIP_TO_DRIVER:
                            breachWarningMessageToSend = "Your trip with reference number "+tripRequest.getReferenceNo()+" which you accepted will soon expire in "+(tripRequest.getExpiredTime().getMinute()-LocalDateTime.now().getMinute())+" mins .You must ASSIGN IT to one of your drivers within the agreed time";
                            
                    }
                    //tripRequestIterator.remove(); // Remove the expired trip from the pool of trips due for expiration.
                    User driverUser = null;
                    User assistantDriverUser = null;
                    if (tripRequest.getDriverId() !=null)
                    {
                        Driver driver = driverRepository.findById(tripRequest.getDriverId())
                                .orElseThrow(()-> new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"The driverId assigend to a tripRequest-that's expiration is due to breached- doesn't exist"));
                        driverUser = userRepository.findById(driver.getUserId())
                                .orElseThrow(()-> new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"The corresponding userId for a driver -whose trips's expiration is due to breached-doesn't exist"));
                    }
                    if (tripRequest.getDriverAssistantId() !=null)
                    {
                        Driver assitantDriver = driverRepository.findById(tripRequest.getDriverAssistantId())
                                .orElseThrow(()-> new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"The assitantDriverId assigend to a tripRequest-whose expiration is due to breached- doesn't exist"));
                        assistantDriverUser = userRepository.findById(assitantDriver.getUserId())
                                .orElseThrow(()-> new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"The corresponding userId for an assitant driver -whose trips's expiration is due to breached-doesn't exist"));
                    }
                    if (partnerUser!=null && driverUser == null && assistantDriverUser == null)
                    {
                        log.info("Sending to Partner ONLY");
                        pushExpiredTripNotificationsToPartnerAndDriver(tripRequest, partnerUser, null,breachWarningMessageToSend , sla.getSlaName(),null);
                    }
                    if (driverUser != null)
                    {
                        log.info("Sending to Driver ONLY");
                        pushExpiredTripNotificationsToPartnerAndDriver(tripRequest, partnerUser, driverUser,breachWarningMessageToSend,sla.getSlaName(),null);
                    }
                    if (assistantDriverUser != null)
                    {
                        log.info("Sending to Assitant Driver ONLY");
                        pushExpiredTripNotificationsToPartnerAndDriver(tripRequest, partnerUser, assistantDriverUser,breachWarningMessageToSend,sla.getSlaName(),null);
                    }
                }
        }
    }

    private void pushExpiredTripNotificationsToPartnerAndDriver(TripRequest tripRequest, User partnerUser, User driverUser, String message, SlaName slaName, String driverType) {

        if(partnerUser!=null){
            if (driverUser== null)
            {
                SLANotifier slaNotifierRequestDto = SLANotifier.builder().slaId(slaService.findSLAByName(slaName).getId())
                        .email(partnerUser.getEmail()).name(partnerUser.getFirstName() + " " + partnerUser.getLastName()).build();
                slaNotifierRequestDto.setIsActive(true);
                log.info("The SLA NotifierRequestDto to create ->{}",slaNotifierRequestDto);

                generalNotificationService.dispatchNotificationsToUser(partnerUser, message);
                SLANotifier slaNotifier = slaNotifierRepository.save(slaNotifierRequestDto);
                log.info("Successfully created  "+slaName+" SLA  Notifier to a partner ->{}", slaNotifier);
            }
            else {
                SLANotifier slaNotifierRequestDto = SLANotifier.builder().slaId(slaService.findSLAByName(slaName).getId())
                        .email(driverUser.getEmail()).name(driverUser.getFirstName() + " " + driverUser.getLastName()).build();
                slaNotifierRequestDto.setIsActive(true);
                log.info("The SLA NotifierRequestDto to create ->{}",slaNotifierRequestDto);

                String msg = "";
                if (slaName.equals(slaName.ACCEPT_TRIP_REQUEST)){
                    msg = "Hello "+ driverUser.getFirstName() + ",you MUST ACCEPT the assigned trip with reference number " + tripRequest.getReferenceNo() + " within "+(tripRequest.getExpiredTime().getMinute()-LocalDateTime.now().getMinute())+" mins before it expires.";
                    generalNotificationService.dispatchNotificationsToUser(driverUser, msg);
                }
                else if (slaName.equals(slaName.START_TRIP)){
                    msg = "Hello "+ driverUser.getFirstName() + ",you MUST START the assigned trip with reference number " + tripRequest.getReferenceNo() + " within "+(tripRequest.getExpiredTime().getMinute()-LocalDateTime.now().getMinute())+" mins before it expires.";
                    generalNotificationService.dispatchNotificationsToUser(driverUser, msg);
                }
                else
                {
                    slaNotifierRequestDto = SLANotifier.builder().slaId(slaService.findSLAByName(slaName).getId())
                            .email(partnerUser.getEmail()).name(partnerUser.getFirstName() + " " + partnerUser.getLastName()).build();
                    slaNotifierRequestDto.setIsActive(true);
                    log.info("The SLA NotifierRequestDto to create ->{}",slaNotifierRequestDto);

                    msg = driverType + driverUser.getFirstName() + " failed to accept the assigned trip with reference number " + tripRequest.getReferenceNo() + " before it expired";
                    generalNotificationService.dispatchNotificationsToUser(partnerUser, msg);
                }
                SLANotifier slaNotifier = slaNotifierRepository.save(slaNotifierRequestDto);
                log.info("Successfully created  "+slaName+" SLA  Notifier to a partner/driver ->{}", slaNotifier);
            }
        }
        else if (partnerUser == null && driverUser!= null){
            SLANotifier slaNotifierRequestDto = SLANotifier.builder().slaId(slaService.findSLAByName(slaName).getId())
                    .email(driverUser.getEmail()).name(driverUser.getFirstName() + " " + driverUser.getLastName()).build();
            slaNotifierRequestDto.setIsActive(true);
            log.info("The SLA NotifierRequestDto to create ->{}",slaNotifierRequestDto);

            generalNotificationService.dispatchNotificationsToUser(driverUser, "Hello " + driverUser.getFirstName() + " since you failed to accept the assigned trip with reference number " + tripRequest.getReferenceNo() + ", it has thus expired.");
            SLANotifier slaNotifier = slaNotifierRepository.save(slaNotifierRequestDto);
            log.info("Successfully created  "+slaName+" SLA  Notifier(after trip expired) to a partner ->{}", slaNotifier);
        }
    }

    public void expireTripsOrPushNotifications(){
        this.triggerNotificationsWarningOfNearBreachesTrips();
        this.expireUnAcceptedPartnersDriversTrips();
        this.expireUnAssignedPartnerTrips();
        this.expireNonStartedDriverTrips();
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

            request.setCurrentSystemTime(LocalDateTime.now());
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
            request.setCurrentSystemTime(LocalDateTime.now());
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
