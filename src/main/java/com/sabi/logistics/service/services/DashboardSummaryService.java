package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.DashBoardSummaryRequest;
import com.sabi.logistics.core.dto.request.TripAssetDto;
import com.sabi.logistics.core.dto.response.DashboardResponseDto;
import com.sabi.logistics.core.dto.response.DriverDashboardResponseDto;
import com.sabi.logistics.core.models.*;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.*;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("ALL")
@Service
public class DashboardSummaryService {
    private static final Logger log = LoggerFactory.getLogger(DashboardSummaryService.class);
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final DriverRepository driverRepository;
    private final PartnerRepository partnerRepository;
    private final UserRepository userRepository;
    @Autowired
    TripRequestRepository tripRequestRepository;
    @Autowired
    DashboardSummaryRepository dashboardSummaryRepository;
    @Autowired
    PartnerAssetRepository partnerAssetRepository;
    @Autowired
    PartnerAssetTypeRepository partnerAssetTypeRepository;
    @Autowired
    AssetTypePropertiesRepository assetTypePropertiesRepository;
    @Autowired
    private Validations validations;

    public DashboardSummaryService(ModelMapper mapper, ObjectMapper objectMapper, DriverRepository driverRepository, PartnerRepository partnerRepository, UserRepository userRepository) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.driverRepository = driverRepository;
        this.partnerRepository = partnerRepository;
        this.userRepository = userRepository;
    }

    @Scheduled(
            cron = "${update.dashboard}"
    )
    public void updateDashboardSummary() {
        Boolean isActive = true;
        LocalDate localDate = LocalDate.now();
        LocalDateTime date = localDate.atStartOfDay();
        this.tripRequestRepository.findByCreatedDate(date);
    }

    public DashboardResponseDto getDashboardSummary(Long partnerId, LocalDateTime startDate, LocalDateTime endDate) {
        List<DashboardSummary> dashboardSummaries = this.dashboardSummaryRepository.getAllBetweenDates(startDate, endDate, partnerId);
        Integer totalCompletedTrips = this.tripRequestRepository.countByPartnerIDAndStatus(partnerId, "Completed", startDate, endDate);
        Integer outstandingTrips = this.tripRequestRepository.countByPartnerIDAndStatus(partnerId, "Pending", startDate, endDate);
        Double totalEarnings = this.getTotalEarnings(dashboardSummaries);
        Double outstandingAmount = this.getOutstandingAmount(dashboardSummaries);
        Boolean isActive = true;
        LocalDate localDate = LocalDate.now();
        LocalDateTime date = localDate.atStartOfDay();
        Integer incomingTrip = this.tripRequestRepository.countByPartnerIdAndDeliveryStatusAndIsActiveAndCreatedDateGreaterThanEqual(partnerId, "Pending", isActive, date);
        Integer cancelledTrip = this.tripRequestRepository.countByPartnerIdAndDeliveryStatusAndIsActiveAndCreatedDateGreaterThanEqual(partnerId, "Cancelled", isActive, date);
        Integer completedTrip = this.tripRequestRepository.countByPartnerIdAndDeliveryStatusAndIsActiveAndCreatedDateGreaterThanEqual(partnerId, "Completed", isActive, date);
        Integer ongoingTrip = this.tripRequestRepository.countByPartnerIdAndDeliveryStatusAndIsActiveAndCreatedDateGreaterThanEqual(partnerId, "Ongoing", isActive, date);
        Integer availablePartnerAsset = this.partnerAssetRepository.countByPartnerId(partnerId, "Available", isActive);
        Integer intransitPartnerAsset = this.partnerAssetRepository.countByPartnerId(partnerId, "Intransit", isActive);
        DashboardResponseDto responseDto = new DashboardResponseDto();
        responseDto.setPartnerId(partnerId);
        responseDto.setIncomingTrip(incomingTrip);
        responseDto.setCancelledTrip(cancelledTrip);
        responseDto.setOngoingTrip(ongoingTrip);
        responseDto.setCompletedTrip(completedTrip);
        responseDto.setTotalCompletedTrips(totalCompletedTrips);
        responseDto.setOutstandingTrips(outstandingTrips);
        responseDto.setTotalEarnings(totalEarnings);
        responseDto.setOutstandingAmount(outstandingAmount);
        responseDto.setAvailablePartnerAsset(availablePartnerAsset);
        responseDto.setInTransitPartnerAsset(intransitPartnerAsset);
        responseDto.setTripAsset(this.getTripToAsset(partnerId, isActive));
        return responseDto;
    }

    private Integer getTotalCompletedTrips(List<DashboardSummary> dashboardSummaries) {
        return (Integer)dashboardSummaries.stream().filter(Objects::nonNull).map(DashboardSummary::getTotalCompletedTrips).reduce(Integer.valueOf(0), Integer::sum);
    }

    private Integer getOutstandingTrips(List<DashboardSummary> dashboardSummaries) {
        return (Integer)dashboardSummaries.stream().filter(Objects::nonNull).map(DashboardSummary::getOutstandingTrips).reduce(Integer.valueOf(0), Integer::sum);
    }

    private Double getTotalEarnings(List<DashboardSummary> dashboardSummaries) {
        return ((BigDecimal)dashboardSummaries.stream().filter(Objects::nonNull).map(DashboardSummary::getTotalEarnings).reduce(BigDecimal.ZERO, BigDecimal::add)).doubleValue();
    }

    private Double getOutstandingAmount(List<DashboardSummary> dashboardSummaries) {
        return ((BigDecimal)dashboardSummaries.stream().filter(Objects::nonNull).map(DashboardSummary::getOutstandingAmount).reduce(BigDecimal.ZERO, BigDecimal::add)).doubleValue();
    }

    public List<TripAssetDto> getTripToAsset(Long partnerId, Boolean isActive) {
        List<TripAssetDto> tripAssetDtos = new ArrayList();
        List<PartnerAsset> partnerAssets = this.partnerAssetRepository.findByIsActiveAndId(partnerId, isActive);
        partnerAssets.forEach((asset) -> {
            Integer trip = this.tripRequestRepository.countByPartnerIdAndPartnerAssetId(partnerId, asset.getId());
            TripAssetDto tripAsset = new TripAssetDto();
            tripAsset.setAssetTypeName(asset.getAssetTypeName());
            tripAsset.setTrip(trip);
            tripAssetDtos.add(tripAsset);
        });
        return tripAssetDtos;
    }

    /**
     * @Description: Returns driver's trips delivery status statistics: failed, completed and partial.
     * @Author: Afam Okonkwo
     * @Date: 21/04/22
     * @param driverId
     * @return
     */

    public DriverDashboardResponseDto getDriverTripsStatistics(Long driverUserId) {
        Driver driver = driverRepository.findByUserId(driverUserId);
        if (driver == null)
           throw  new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "This driver does not exist");
        Partner partner = partnerRepository.findById(driver.getPartnerId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The driver's partnerId does not exist"));

        User partnerUser = userRepository.findById(partner.getUserId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The user Id of the driver's partner does not exist"));

        User driverUser = userRepository.findById(driver.getUserId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The driver's userId doesn't exist"));

        DriverDashboardResponseDto driverDashboardResponseDto = new DriverDashboardResponseDto();
        driverDashboardResponseDto.setDriverId(driver.getId());
        driverDashboardResponseDto.setPartnerId(driver.getPartnerId());
        driverDashboardResponseDto.setDriverName(driverUser.getFirstName()+" "+driverUser.getLastName());
        driverDashboardResponseDto.setPartnerName(partnerUser.getFirstName()+" "+partnerUser.getLastName());

        BigInteger totalCompletedTrips = tripRequestRepository.countByDriverIdAndDeliveryStatus(driver.getId(),"Completed");
        BigInteger partialCompletedTrips = tripRequestRepository.countByDriverIdAndDeliveryStatus(driver.getId(),"PartiallyCompleted");
        BigInteger totalFailedTrips = tripRequestRepository.countByDriverIdAndDeliveryStatus(driver.getId(),"failed");
        driverDashboardResponseDto.setTotalCompletedTrips(totalCompletedTrips);
        driverDashboardResponseDto.setTotalFailedTrips(totalFailedTrips);
        driverDashboardResponseDto.setTotalPartiallyCompletedTrips(partialCompletedTrips);

        return driverDashboardResponseDto;

    }


    public void moveTripRecordToDashBoard()  {
        List<TripRequest> tripRequests = tripRequestRepository.listTrips();
        for (TripRequest tran : tripRequests
                ) {
            DashboardSummary dashboardSummary = DashboardSummary.builder()
                    .partnerId(tran.getPartnerId())
                    .deliveryStatus(tran.getDeliveryStatus())
                    .date(tran.getCreatedDate())
                    .assetTypeId(tran.getPartnerAssetId())
                    .referenceNo(tran.getReferenceNo())
                    .earnings(tran.getEarnings())
                    .build();
            DashboardSummary dashboard = dashboardSummaryRepository.findByPartnerIdAndReferenceNo(dashboardSummary.getPartnerId(),dashboardSummary.getReferenceNo());
            if(dashboard == null) {
                dashboardSummaryRepository.save(dashboardSummary);
            }

        }
    }







    public List<DashBoardSummaryRequest> dashBoardSummary(Long partnerId,LocalDateTime start,LocalDateTime end) {
        List<DashBoardSummaryRequest> resultLists = new ArrayList<>();
        List<Object[]> result = dashboardSummaryRepository.GetTotalTripsAndTotalEarnings(partnerId,start,end);

        try{
            result.forEach(r -> {
                DashBoardSummaryRequest resultList = new DashBoardSummaryRequest();

//                BigInteger partner = (BigInteger) r[0];
//                BigInteger completeTrip = (BigInteger) r[1];
//                BigDecimal totalEarnings = (BigDecimal) r[2];
                resultList.setPartnerId((Long) r[0]);
                resultList.setCompletedTrips((Long) r[1]);
                resultList.setTotalEarnings((BigDecimal) r[2]);
                resultList.setDate((LocalDateTime) r[3]);
//                BigInteger assetType = (BigInteger) r[4];
//                resultList.setAssetType(assetType.longValue());
                resultLists.add(resultList);

            });

        }catch (Exception e){
            e.printStackTrace();

        }
        return resultLists;

    }



    public List<DashBoardSummaryRequest> dashBoardOutStandingSummary(Long partnerId,LocalDateTime start,LocalDateTime end) {
        List<DashBoardSummaryRequest> resultLists = new ArrayList<>();
        List<Object[]> result1 = dashboardSummaryRepository.GetOutStandingTripsAndOutStandEarnings(partnerId,start,end);

        try{
            result1.forEach(r -> {
                DashBoardSummaryRequest resultList1 = new DashBoardSummaryRequest();
                resultList1.setPartnerId((Long) r[0]);
                resultList1.setOutStandingTrips((Long) r[1]);
                resultList1.setOutStandingEarnings((BigDecimal) r[2]);
                resultList1.setDate((LocalDateTime) r[3]);
//                resultList1.setAssetType(assetType.longValue());
                resultLists.add(resultList1);
            });

        }catch (Exception e){
            e.printStackTrace();
        }
        return resultLists;

    }


}





