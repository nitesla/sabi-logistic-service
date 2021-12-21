package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabi.logistics.core.dto.request.TripAssetDto;
import com.sabi.logistics.core.dto.response.DashboardResponseDto;
import com.sabi.logistics.core.models.DashboardSummary;
import com.sabi.logistics.core.models.PartnerAsset;
import com.sabi.logistics.service.repositories.*;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class DashboardSummaryService {
    private static final Logger log = LoggerFactory.getLogger(DashboardSummaryService.class);
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
}





