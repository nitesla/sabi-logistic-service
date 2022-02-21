package com.sabi.logistics.service.integrations;

import com.sabi.framework.helpers.API;
import com.sabi.framework.service.ExternalTokenService;
import com.sabi.logistics.core.dto.request.SabiOrder;
import com.sabi.logistics.core.dto.response.ExternalWebServiceResponse;
import com.sabi.logistics.core.integrations.response.SingleOrderResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ExternalWebService {

    @Value("${remote.web.service.fingerprint}")
    private String remoteServiceFingerprint;
    @Value("${remote.web.service.dateSearch}")
    private String dateSearch;
    @Value("${remote.web.service.single.order.url}")
    private String singelOrderPath;
    private final API api;

    private final ExternalTokenService externalTokenService;

    public ExternalWebService(API api, ExternalTokenService externalTokenService) {
        this.api = api;
        this.externalTokenService =externalTokenService;
    }

    public SingleOrderResponse orderDetail (Long orderId) throws IOException {

        Map headersMap=new HashMap();
        headersMap.put("fingerprint",remoteServiceFingerprint);
        headersMap.put("Authorization","Bearer"+ " " +externalTokenService.getToken());
        SingleOrderResponse response = api.get(singelOrderPath+ orderId, SingleOrderResponse.class,headersMap);
        return response;
    }

    public ExternalWebServiceResponse getExternalOrders(String path, LocalDateTime lastSyncTime ){
        Map<String, String> headers= new HashMap<>();
        headers.put("Authorization","Bearer "+externalTokenService.getToken());
        headers.put("fingerprint",remoteServiceFingerprint);
        StringBuilder requestParams = new StringBuilder(path);
        requestParams.append("?dateSearch=");
        requestParams.append(dateSearch);
        requestParams.append("&fromDate=");
        requestParams.append(lastSyncTime.format(DateTimeFormatter.ofPattern("dd/MM/YYYY")));
        //requestParams.append("02-02-2022");
        requestParams.append("&toDate=");
        requestParams.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/YYYY")));
        return (ExternalWebServiceResponse) api.get(requestParams.toString(),ExternalWebServiceResponse.class,headers);
    }
}
