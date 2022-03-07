package com.sabi.logistics.service.integrations;

import com.sabi.logistics.core.dto.request.SabiOrder;
import com.sabi.logistics.core.dto.response.ExternalWebServiceResponse;
import com.sabi.logistics.core.integrations.order.OrderItem;
import com.sabi.logistics.core.integrations.response.SingleOrderResponse;
import com.sabi.logistics.core.models.Order;
import com.sabi.logistics.service.repositories.OrderItemRepository;
import com.sabi.logistics.service.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncOrderService {

    private final ExternalWebService externalWebService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;



    public void syncAndPullExternalOrders(String requestUrl) throws IOException {
        log.info("LastOrder ID::{}",orderRepository.getLastOrder());
        Long lastOrderId = orderRepository.getLastOrder();
        if(lastOrderId!=null)
        {
            Order lastOrder = orderRepository.findOrderById(lastOrderId);
            if (lastOrder.getUpdatedDate().getDayOfYear() != LocalDateTime.now().getDayOfYear())
            {
                ExternalWebServiceResponse webServiceResponse = externalWebService.getExternalOrders(requestUrl, lastOrder.getUpdatedDate());
                log.info("Returned Requests from Sabi::{}",webServiceResponse);
                List<Order> orderList = new ArrayList<>(); lastOrder.getUpdatedDate();
                if(webServiceResponse!=null && webServiceResponse.isStatus() && webServiceResponse.getData().size() > 0){
                    for (SabiOrder sabiOrder: webServiceResponse.getData()){
                        Order order = new Order();
                        order.setCustomerName(sabiOrder.getUserName());
                        order.setCustomerPhone(sabiOrder.getPhoneNumber());
                        order.setCreatedDate(LocalDateTime.parse(sabiOrder.getCreatedDate().toString()) );
                        order.setDeliveryStatus(sabiOrder.getDeliveryState());

                        order.setReferenceNo(sabiOrder.getOrderNumber());
                        order.setTotalAmount(sabiOrder.getTotalPrice());
                        order.setTotalQuantity(sabiOrder.getNoOfItems());
                        order.setThirdPartyOrderId(sabiOrder.getId());
                        order.setIsActive(true);
                        order = (Order)orderRepository.save(order);
                        log.info("Synced Order successfully saved!",order);
                        SingleOrderResponse singleOrderResponse = externalWebService.orderDetail(sabiOrder.getId());
                        if (singleOrderResponse!=null && singleOrderResponse.isStatus() && singleOrderResponse.getData()!=null){
                            List<OrderItem> orderItemList = singleOrderResponse.getData().getOrderItems();
                            BigDecimal totalOrderAmount = new BigDecimal(0);
                            if(orderItemList.size() > 0){
                                for (OrderItem orderItem: orderItemList){
                                    com.sabi.logistics.core.models.OrderItem logisticsOrderItem = new com.sabi.logistics.core.models.OrderItem();
                                    logisticsOrderItem.setUnitPrice(new BigDecimal(orderItem.getUnitPrice()));
                                    logisticsOrderItem.setThirdPartyProductId(orderItem.getProductId());
                                    logisticsOrderItem.setQty(orderItem.getQuantity());
                                    logisticsOrderItem.setProductName(orderItem.getProductName());
                                    logisticsOrderItem.setDeliveryStatus("pending");
                                    BigDecimal totalOrderItemsAmount = new BigDecimal(orderItem.getQuantity()).multiply(new BigDecimal(orderItem.getUnitPrice()));
                                    logisticsOrderItem.setTotalPrice(totalOrderItemsAmount);
                                    totalOrderAmount.add(totalOrderItemsAmount);
                                    logisticsOrderItem.setDeliveryAddress(singleOrderResponse.getData().getOrderDelivery().getAddress());
                                    logisticsOrderItem.setCustomerPhone(singleOrderResponse.getData().getCustomerDetails().getPhoneNumber());
                                    logisticsOrderItem.setCustomerName(singleOrderResponse.getData().getCustomerDetails().getName());
                                    logisticsOrderItem.setOrderId(order.getId());
                                    logisticsOrderItem.setCreatedDate(LocalDateTime.parse(singleOrderResponse.getData().getOrderDate().toString()));
                                    logisticsOrderItem.setThirdPartyOrderId(orderItem.getOrderItemId());
                                    logisticsOrderItem.setIsActive(true);
                                    //We still need to set weight,height and length properties
                                    orderItemRepository.save(logisticsOrderItem);
                                    log.info("Synced OrderItems successfully saved!{}",logisticsOrderItem);

                                }
                            }
                            order.setTotalAmount(totalOrderAmount.doubleValue());
                            order=(Order) orderRepository.save(order);
                        }

                    }
                    log.info("Synced Orders successfully saved!");
                }

            }
            log.info("OOOps!, Sorry Order for today {} is already synced. Today's date is {}",lastOrder.getUpdatedDate(),LocalDateTime.now());

        }
        else {
            log.info("OOOps!, no existing order yet in the DB and hence no previous time to sync with external webservice");
        }

    }
}
