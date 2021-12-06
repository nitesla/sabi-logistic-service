package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.DashboardRequestDto;
import com.sabi.logistics.core.dto.request.TripAssetDto;
import com.sabi.logistics.core.dto.response.DashboardResponseDto;
import com.sabi.logistics.core.models.*;
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



    public DashboardResponseDto getDashboardSummary(DashboardRequestDto request) {
        List<DashboardSummary> dashboardSummaries = dashboardSummaryRepository.getAllBetweenDates(request.getStartDate(), request.getEndDate(), request.getPartnerId());
        Integer totalCompletedTrips = getTotalCompletedTrips(dashboardSummaries);
        Integer outstandingTrips = getOutstandingTrips(dashboardSummaries);
        Double totalEarnings = getTotalEarnings(dashboardSummaries);
        Double outstandingAmount = getOutstandingAmount(dashboardSummaries);

        Boolean isActive = true;
        LocalDate localDate = LocalDate.now();
        LocalDateTime date = localDate.atStartOfDay();
        Integer incomingTrip = tripRequestRepository.countByPartnerIDAndDeliveryStatusAndIsActiveAndCreatedDateGreaterThanEqual(request.getPartnerId(), StatusConstants.PENDING, isActive, date);
        Integer cancelledTrip = tripRequestRepository.countByPartnerIDAndDeliveryStatusAndIsActiveAndCreatedDateGreaterThanEqual(request.getPartnerId(), StatusConstants.CANCELLED, isActive, date);
        Integer completedTrip = tripRequestRepository.countByPartnerIDAndDeliveryStatusAndIsActiveAndCreatedDateGreaterThanEqual(request.getPartnerId(), StatusConstants.COMPLETED, isActive, date);
        Integer ongoingTrip = tripRequestRepository.countByPartnerIDAndDeliveryStatusAndIsActiveAndCreatedDateGreaterThanEqual(request.getPartnerId(), StatusConstants.ONGOING, isActive, date);

        DashboardResponseDto responseDto = new DashboardResponseDto();

        responseDto.setPartnerId(request.getPartnerId());

        responseDto.setIncomingTrip(incomingTrip);
        responseDto.setCancelledTrip(cancelledTrip);
        responseDto.setOutgoingTrip(ongoingTrip);
        responseDto.setCompletedTrip(completedTrip);


        responseDto.setTotalCompletedTrips(totalCompletedTrips);
        responseDto.setOutstandingTrips(outstandingTrips);
        responseDto.setTotalEarnings(totalEarnings);
        responseDto.setOutstandingAmount(outstandingAmount);

        responseDto.setTripAsset(getTripToAsset(request.getPartnerId(), isActive));


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

            PartnerAssetType partnerAssetType = partnerAssetTypeRepository.findPartnerAssetTypeById(asset.getPartnerAssetTypeId());
            if (partnerAssetType == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid PartnerAssetType Id");
            }

            AssetTypeProperties assetTypeProperties = assetTypePropertiesRepository.findAssetTypePropertiesById(partnerAssetType.getAssetTypeId());
            if (assetTypeProperties == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid AssetTypeProperties Id");
            }

            asset.setAssetTypeName(assetTypeProperties.getName());

            TripAssetDto tripAsset = new TripAssetDto();

            tripAsset.setAssetTypeName(asset.getAssetTypeName());
            tripAsset.setTrip(trip);

            tripAssetDtos.add(tripAsset);

        });

         return tripAssetDtos;

    }

}
