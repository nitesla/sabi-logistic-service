package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.utils.CustomResponseCode;
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
    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;



    public FulfilmentDashboard findAll(String deliveryStatus,Long wareHouseId) {
        Warehouse warehouse = warehouseRepository.findWarehouseById(wareHouseId);
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        SimpleDateFormat dateFormatter2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Date currentDate = new Date();
        Date currentDate1 = new Date();

        String endDate = dateFormatter.format(currentDate);
        Calendar cal = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(currentDate1);
        cal2.add(Calendar.HOUR, -24);
        Date endDay = cal2.getTime();
        String startDate = dateFormatter.format(endDay);
        String strDate1 = String.valueOf(cal2);
        log.info("startDate ::::::::::::::::::::: " + startDate);
        log.info("endDate ::::::::::::::::::::: " + endDate);
        LocalDateTime dateTime = LocalDateTime.parse(startDate, format);
        LocalDateTime dateTime2 = LocalDateTime.parse(endDate, format);
        if (warehouse == null){
           throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Record for Warehouse not found!");
        }
        List<Order> completedOrder = orderRepository.findOrderByCreatedDateAAndDeliveryStatus(dateTime,dateTime2,"Completed");
      int completedCount =  completedOrder.size();
        log.info("completed :::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: " + completedOrder);
        List<Order> pendingOrder = orderRepository.findOrderByCreatedDateAAndDeliveryStatus(dateTime,dateTime2,"Pending");
        int pendingCount =  pendingOrder.size();
        log.info("pending ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: " + pendingOrder);
        List<Order> incomingOrder = orderRepository.findOrderByCreatedDateAAndDeliveryStatus(dateTime,dateTime2,"Ongoing");
        int incomingCount =  incomingOrder.size();
        log.info("ongoing ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: " + incomingOrder);
        List<Order> cancelledOrder = orderRepository.findOrderByCreatedDateAAndDeliveryStatus(dateTime,dateTime2,"Cancelled");
        int cancelledCount =  cancelledOrder.size();
        log.info("cancelled ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: " + cancelledOrder);
        List<Order> orders = orderRepository.findOrderByCreatedDate(dateTime,dateTime2);
        FulfilmentDashboard dashboard = new FulfilmentDashboard();
        log.info("order ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: " + orders);
        orders.forEach(order -> {
           double totalSum = order.getTotalAmount();
//            int totalCount =  orders.size();
//            for (int i=0; i<totalCount; i++)
           totalSum += totalSum;
            log.info("total sum ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: " + totalSum);
            dashboard.setTotalAmount(BigDecimal.valueOf(totalSum));

        });
//        List<Order> savedOrder = orderRepository.findByDate(dateTime,dateTime2);
//        log.info("TodayYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY  " +savedOrder);
        dashboard.setProcessedOrders(completedCount);
        dashboard.setPendingDeliveries(pendingCount);
        dashboard.setCanceledRequest(cancelledCount);
        dashboard.setIncomingRequest(incomingCount);
        dashboard.setProcessedRequest(completedCount);
        dashboard.setPendingRequest(pendingCount);
        dashboard.setWareHouseId(warehouse.getId());
        dashboard.setPartnerId(warehouse.getPartnerId());
        dashboard.setDate(dateTime2);
        repository.save(dashboard);
        return dashboard;

    }

    public List<FulfilmentDashboard> findRecordByDateRange(String date,String endDate){
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(date, format);
        LocalDateTime dateTime2 = LocalDateTime.parse(endDate, format);
        List<FulfilmentDashboard> saveInfo  = repository.findByDate(dateTime,dateTime2);
        if (saveInfo == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Record Not found");
        }
        return saveInfo;
    }


}
