package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabi.logistics.service.repositories.DashboardSummaryRepository;
import com.sabi.logistics.service.repositories.StateRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;


/**
 *
 * This class is responsible for all business logic for Dashboard
 */


@Slf4j
@Service
public class DashboardSummaryService {


    private DashboardSummaryRepository dashboardSummaryRepository;
    private StateRepository stateRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;

    public DashboardSummaryService(ModelMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    //schedule a job to add object in DB (Every 12am)



}
