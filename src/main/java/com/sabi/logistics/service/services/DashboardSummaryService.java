package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabi.logistics.service.repositories.DashboardSummaryRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;


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
    DashboardSummaryRepository dashboardSummaryRepository;



    public DashboardSummaryService(ModelMapper mapper, ObjectMapper objectMapper,
                                   DashboardSummaryRepository dashboardSummaryRepository) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.dashboardSummaryRepository = dashboardSummaryRepository;
    }







}
