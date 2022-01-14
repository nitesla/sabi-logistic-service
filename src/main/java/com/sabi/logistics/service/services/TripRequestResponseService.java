package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.TripRequestResponseReqDto;
import com.sabi.logistics.core.dto.response.TripRequestResponseDto;
import com.sabi.logistics.core.models.Partner;
import com.sabi.logistics.core.models.TripRequestResponse;
import com.sabi.logistics.service.helper.GenericSpecification;
import com.sabi.logistics.service.helper.SearchCriteria;
import com.sabi.logistics.service.helper.SearchOperation;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.PartnerRepository;
import com.sabi.logistics.service.repositories.TripRequestResponseRepository;
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
public class TripRequestResponseService {
    private final TripRequestResponseRepository tripRequestResponseRepository;
    private final ModelMapper mapper;
    @Autowired
    private Validations validations;

    @Autowired
    private PartnerRepository partnerRepository;


    public TripRequestResponseService(TripRequestResponseRepository tripRequestResponseRepository, ModelMapper mapper) {
        this.tripRequestResponseRepository = tripRequestResponseRepository;
        this.mapper = mapper;
    }

    public TripRequestResponseDto createTripRequestResponse(TripRequestResponseReqDto request) {
        validations.validateTripRequestResponse(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        TripRequestResponse requestResponse = mapper.map(request,TripRequestResponse.class);

        TripRequestResponse requestResponseExists = tripRequestResponseRepository.findByTripRequestIdAndPartnerId(requestResponse.getTripRequestId(), requestResponse.getPartnerId());


        if(requestResponseExists != null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Trip Request Response already exist");
        }

        Partner partner = partnerRepository.getOne(request.getPartnerId());

        requestResponse.setCreatedBy(userCurrent.getId());
        requestResponse.setIsActive(true);
        requestResponse = tripRequestResponseRepository.save(requestResponse);
        log.debug("Create new tripRequestResponse - {}"+ new Gson().toJson(requestResponse));
        TripRequestResponseDto requestResponseDto  = mapper.map(requestResponse, TripRequestResponseDto.class);
        requestResponseDto.setPartnerName(partner.getName());

        return requestResponseDto;
    }

    public TripRequestResponseDto updateTripRequestResponse(TripRequestResponseReqDto request) {
        validations.validateTripRequestResponse(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        TripRequestResponse requestResponse = tripRequestResponseRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested requestResponse Id does not exist!"));
        mapper.map(request, requestResponse);

        requestResponse.setUpdatedBy(userCurrent.getId());
        tripRequestResponseRepository.save(requestResponse);
        log.debug("requestResponse record updated - {}"+ new Gson().toJson(requestResponse));
        TripRequestResponseDto requestResponseDto = mapper.map(requestResponse, TripRequestResponseDto.class);

        if(request.getPartnerId() != null ) {
            Partner partner = partnerRepository.getOne(request.getPartnerId());
            requestResponseDto.setPartnerName(partner.getName());
        }
        return requestResponseDto;

    }

    public TripRequestResponseDto findTripRequestResponse(Long id){
        TripRequestResponse requestResponse  = tripRequestResponseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested requestResponseId does not exist!"));
        return mapper.map(requestResponse, TripRequestResponseDto.class);
    }


    public Page<TripRequestResponse> findAll(Long tripRequestId, Long partnerId, String status, PageRequest pageRequest ){

        Page<TripRequestResponse> requestResponses = tripRequestResponseRepository.findTripRequestResponse(tripRequestId, partnerId, status, pageRequest);
        if(requestResponses == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return requestResponses;

    }



    public void enableDisable (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        TripRequestResponse requestResponse  = tripRequestResponseRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested TripRequestResponse Id does not exist!"));
        requestResponse.setIsActive(request.isActive());
        requestResponse.setUpdatedBy(userCurrent.getId());
        tripRequestResponseRepository.save(requestResponse);

    }


    public List<TripRequestResponse> getAll(Boolean isActive){
        List<TripRequestResponse> requestResponses = tripRequestResponseRepository.findByIsActive(isActive);
        return requestResponses;

    }
}
