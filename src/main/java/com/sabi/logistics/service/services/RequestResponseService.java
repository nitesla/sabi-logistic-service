package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.RequestResponseRequestDto;
import com.sabi.logistics.core.dto.response.RequestResponseDto;
import com.sabi.logistics.core.models.Partner;
import com.sabi.logistics.core.models.RequestResponse;
import com.sabi.logistics.service.helper.GenericSpecification;
import com.sabi.logistics.service.helper.SearchCriteria;
import com.sabi.logistics.service.helper.SearchOperation;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.PartnerRepository;
import com.sabi.logistics.service.repositories.RequestResponseRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@SuppressWarnings("All")
@Service
@Slf4j
public class RequestResponseService {
    private final RequestResponseRepository requestResponseRepository;
    private final ModelMapper mapper;
    @Autowired
    private Validations validations;

    @Autowired
    private PartnerRepository partnerRepository;


    public RequestResponseService(RequestResponseRepository requestResponseRepository, ModelMapper mapper) {
        this.requestResponseRepository = requestResponseRepository;
        this.mapper = mapper;
    }

    public RequestResponseDto createRequestResponse(RequestResponseRequestDto request) {
        validations.validateRequestResponse(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        RequestResponse requestResponse = mapper.map(request,RequestResponse.class);

        RequestResponse requestResponseExists = requestResponseRepository.findByTripRequestIDAndPartnerID(requestResponse.getTripRequestID(), requestResponse.getPartnerID());


        if(requestResponseExists != null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Request Response already exist");
        }

        Partner partner = partnerRepository.getOne(request.getPartnerID());

        requestResponse.setCreatedBy(userCurrent.getId());
        requestResponse.setIsActive(true);
        requestResponse = requestResponseRepository.save(requestResponse);
        log.debug("Create new requestResponse - {}"+ new Gson().toJson(requestResponse));
        RequestResponseDto requestResponseDto  = mapper.map(requestResponse, RequestResponseDto.class);
        requestResponseDto.setPartnerName(partner.getName());

        return requestResponseDto;
    }

    public RequestResponseDto updateRequestResponse(RequestResponseRequestDto request) {
        validations.validateRequestResponse(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        RequestResponse requestResponse = requestResponseRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested requestResponse Id does not exist!"));
        mapper.map(request, requestResponse);

        requestResponse.setUpdatedBy(userCurrent.getId());
        requestResponseRepository.save(requestResponse);
        log.debug("requestResponse record updated - {}"+ new Gson().toJson(requestResponse));
        RequestResponseDto requestResponseDto = mapper.map(requestResponse, RequestResponseDto.class);

        if(request.getPartnerID() != null ) {
            Partner partner = partnerRepository.getOne(request.getPartnerID());
            requestResponseDto.setPartnerName(partner.getName());
        }
        return requestResponseDto;

    }

    public RequestResponseDto findRequestResponse(Long id){
        RequestResponse requestResponse  = requestResponseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested requestResponseId does not exist!"));
        return mapper.map(requestResponse, RequestResponseDto.class);
    }


    public Page<RequestResponse> findAll(Long tripRequest, Long partnerID, String status, PageRequest pageRequest ){
        GenericSpecification<RequestResponse> genericSpecification = new GenericSpecification<RequestResponse>();

        if (tripRequest != null)
        {
            genericSpecification.add(new SearchCriteria("tripRequest", tripRequest, SearchOperation.EQUAL));
        }

        if (partnerID != null)
        {
            genericSpecification.add(new SearchCriteria("partnerID", partnerID, SearchOperation.EQUAL));
        }

        if (status != null && !status.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("status", status, SearchOperation.EQUAL));
        }



        Page<RequestResponse> requestResponses = requestResponseRepository.findAll(genericSpecification, pageRequest);
        if(requestResponses == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return requestResponses;

    }



    public void enableDisable (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        RequestResponse requestResponse  = requestResponseRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested RequestResponse Id does not exist!"));
        requestResponse.setIsActive(request.isActive());
        requestResponse.setUpdatedBy(userCurrent.getId());
        requestResponseRepository.save(requestResponse);

    }


    public List<RequestResponse> getAll(Boolean isActive){
        List<RequestResponse> requestResponses = requestResponseRepository.findByIsActive(isActive);
        return requestResponses;

    }
}
