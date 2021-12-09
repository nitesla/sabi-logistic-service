package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabi.logistics.core.dto.request.TripAssetDto;
import com.sabi.logistics.core.dto.response.DashboardResponseDto;
import com.sabi.logistics.core.models.DashboardSummary;
import com.sabi.logistics.core.models.PartnerAsset;
import com.sabi.logistics.core.models.TripRequest;
import com.sabi.logistics.service.helper.StatusConstants;
import com.sabi.logistics.service.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 *
 * This class is responsible for all business logic for Dashboard
 */

@SuppressWarnings("All")
@Slf4j
@Service
public class DashboardSummaryService {

    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;

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

    public DashboardSummaryService(ModelMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    //schedule a job to add object in DB (Every 12am)
    @Scheduled(cron = "${update.dashboard}")
    public void updateDashboardSummary() {
        Boolean isActive = true;
        LocalDate localDate = LocalDate.now();
        LocalDateTime date = localDate.atStartOfDay();

        List<TripRequest> tripRequest = tripRequestRepository.findByCreatedDate(date);



    }



    public DashboardResponseDto getDashboardSummary(Long partnerId, LocalDateTime startDate, LocalDateTime endDate) {
        List<DashboardSummary> dashboardSummaries = dashboardSummaryRepository.getAllBetweenDates(startDate, endDate, partnerId);
        Integer totalCompletedTrips = tripRequestRepository.countByPartnerIDAndStatus(partnerId, StatusConstants.COMPLETED,startDate, endDate);
        Integer outstandingTrips = tripRequestRepository.countByPartnerIDAndStatus(partnerId, StatusConstants.PENDING, startDate, endDate);

        Double totalEarnings = getTotalEarnings(dashboardSummaries);
        Double outstandingAmount = getOutstandingAmount(dashboardSummaries);

        Boolean isActive = true;
        LocalDate localDate = LocalDate.now();
        LocalDateTime date = localDate.atStartOfDay();
        Integer incomingTrip = tripRequestRepository.countByPartnerIDAndDeliveryStatusAndIsActiveAndCreatedDateGreaterThanEqual(partnerId, StatusConstants.PENDING, isActive, date);
        Integer cancelledTrip = tripRequestRepository.countByPartnerIDAndDeliveryStatusAndIsActiveAndCreatedDateGreaterThanEqual(partnerId, StatusConstants.CANCELLED, isActive, date);
        Integer completedTrip = tripRequestRepository.countByPartnerIDAndDeliveryStatusAndIsActiveAndCreatedDateGreaterThanEqual(partnerId, StatusConstants.COMPLETED, isActive, date);
        Integer ongoingTrip = tripRequestRepository.countByPartnerIDAndDeliveryStatusAndIsActiveAndCreatedDateGreaterThanEqual(partnerId, StatusConstants.ONGOING, isActive, date);

        Integer availablePartnerAsset = partnerAssetRepository.countByPartnerId(partnerId, StatusConstants.AVAILABLE, isActive);
        Integer intransitPartnerAsset = partnerAssetRepository.countByPartnerId(partnerId, StatusConstants.INTRANSIT, isActive);




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

        responseDto.setTripAsset(getTripToAsset(partnerId, isActive));


        return responseDto;
    }

    private Integer getTotalCompletedTrips(List<DashboardSummary> dashboardSummaries){
        return dashboardSummaries
                .stream()
                .filter(Objects::nonNull)
                .map(DashboardSummary::getTotalCompletedTrips)
                .reduce(0,Integer::sum);
    }

    private Integer getOutstandingTrips(List<DashboardSummary> dashboardSummaries){
        return dashboardSummaries
                .stream()
                .filter(Objects::nonNull)
                .map(DashboardSummary::getOutstandingTrips)
                .reduce(0,Integer::sum);
    }

    private Double getTotalEarnings(List<DashboardSummary> dashboardSummaries){
        return dashboardSummaries
                .stream()
                .filter(Objects::nonNull)
                .map(DashboardSummary::getTotalEarnings)
                .reduce(BigDecimal.ZERO,BigDecimal::add)
                .doubleValue();
    }

    private Double getOutstandingAmount(List<DashboardSummary> dashboardSummaries){
        return dashboardSummaries
                .stream()
                .filter(Objects::nonNull)
                .map(DashboardSummary::getOutstandingAmount)
                .reduce(BigDecimal.ZERO,BigDecimal::add)
                .doubleValue();
    }

    public List<TripAssetDto> getTripToAsset(Long partnerId,Boolean isActive){

        List<TripAssetDto> tripAssetDtos = new ArrayList<>();
        List<PartnerAsset> partnerAssets = partnerAssetRepository.findByIsActiveAndId(partnerId,isActive);


        partnerAssets.forEach(asset->{

            Integer trip = tripRequestRepository.countByPartnerIDAndPartnerAssetID(partnerId, asset.getId());

//            PartnerAssetType partnerAssetType = partnerAssetTypeRepository.findPartnerAssetTypeById(asset.getPartnerAssetTypeId());
//            if (partnerAssetType == null) {
//                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid PartnerAssetType Id");
//            }
//
//            AssetTypeProperties assetTypeProperties = assetTypePropertiesRepository.findAssetTypePropertiesById(partnerAssetType.getAssetTypeId());
//            if (assetTypeProperties == null) {
//                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid AssetTypeProperties Id");
//            }

//            asset.setAssetTypeName(asset.getName());

            TripAssetDto tripAsset = new TripAssetDto();

            tripAsset.setAssetTypeName(asset.getAssetTypeName());
            tripAsset.setTrip(trip);

            tripAssetDtos.add(tripAsset);

        });

         return tripAssetDtos;

    }

}
