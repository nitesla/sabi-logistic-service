package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.response.FulfilmentDashBoardResponseDto;
import com.sabi.logistics.core.models.*;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@SuppressWarnings("ALL")
@Slf4j
@Service
public class FulfilmentDashboardService {

    @Autowired
    private  ModelMapper mapper;
    @Autowired
    private  ObjectMapper objectMapper;
    @Autowired
    private  Validations validations;
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private FulfilmentDashboardRepository repository;

//    @Autowired
//    private InventoryRepository inventoryRepository;
//
//    @Autowired
//    private WarehouseRepository warehouseRepository;
//
//    @Autowired
//    private OrderItemRepository orderItemRepository;



//    public FulfilmentDashboard findAll(Long wareHouseId, String dateFrom, String dateTo) {
//        Warehouse warehouse = warehouseRepository.findWarehouseById(wareHouseId);
//        if (warehouse == null){
//            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"warehose not found!");
//        }
//        List<OrderItem> savedOrderItem = orderItemRepository.findOrderItems(warehouse.getId());
//        log.info("pending :::::::::::::::::::::: " + savedOrderItem);
//            boolean isActive = true;
//            FulfilmentDashboard dashboard = new FulfilmentDashboard();
//            LocalDate localDate = LocalDate.now();
//            LocalDateTime date = localDate.atStartOfDay();
//            log.info("Date :::::::::::::::::::::: " + date);
//            savedOrderItem.forEach(orderItem -> {
//                Integer pendingRequest = orderRepository.countByIdAndDeliveryStatusAndIsActiveAndCreatedDateGreaterThanEqual(orderItem.getId(), "pending", isActive, date);
//                dashboard.setPendingRequest(pendingRequest);
//                log.info("pending :::::::::::::::::::::: " + pendingRequest);
//            });
////            Order savedOrder = orderRepository.getOne(orderItem.getId());
////            Integer pendingRequest = orderRepository.countByIdAndDeliveryStatusAndIsActiveAndCreatedDateGreaterThanEqual(orderItem.getId(), "Pending", isActive, date);
////            dashboard.setPendingRequest(pendingRequest);
//            repository.save(dashboard);
////        FulfilmentDashBoardResponseDto responseDto = new FulfilmentDashBoardResponseDto();
////        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
////        LocalDateTime dateTime3 = LocalDateTime.parse(dateFrom, format);
////        LocalDateTime dateTime4 = LocalDateTime.parse(dateTo, format);
//        return dashboard;
//    }

    public List<FulfilmentDashboard> findRecordByDateRange(String date,String endDate,Long wareHouseId,Long partnerId){
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(date, format);
        LocalDateTime dateTime2 = LocalDateTime.parse(endDate, format);
        List<FulfilmentDashboard> saveInfo  = repository.findFulfilmentDashboardInfo(dateTime,dateTime2,wareHouseId,partnerId);
        if (saveInfo == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Record Not found");
        }
        return saveInfo;
    }


}
