package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.TripRequestDto;
import com.sabi.logistics.core.dto.response.TripResponseDto;
import com.sabi.logistics.core.models.*;
import com.sabi.logistics.service.helper.GenericSpecification;
import com.sabi.logistics.service.helper.SearchCriteria;
import com.sabi.logistics.service.helper.SearchOperation;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.*;
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
public class TripRequestService {
    private final TripRequestRepository tripRequestRepository;
    private final ModelMapper mapper;
    @Autowired
    private Validations validations;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private PartnerAssetRepository partnerAssetRepository;

    @Autowired
    private TripItemRepository tripItemRepository;

    @Autowired
    private TripRequestResponseRepository tripRequestResponseRepository;

    public TripRequestService(TripRequestRepository tripRequestRepository, ModelMapper mapper) {
        this.tripRequestRepository = tripRequestRepository;
        this.mapper = mapper;
    }

    public TripResponseDto createTripRequest(TripRequestDto request) {
        validations.validateTripRequest(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        TripRequest tripRequest = mapper.map(request,TripRequest.class);

        tripRequest.setReferenceNo(validations.generateReferenceNumber(10));
        TripRequest tripRequestExists = tripRequestRepository.findByPartnerAssetIDAndPartnerID(tripRequest.getPartnerID(), tripRequest.getPartnerAssetID());
        if(tripRequestExists != null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Trip Request already exist");
        }

        Partner partner = partnerRepository.getOne(request.getPartnerID());
        PartnerAsset partnerAsset = partnerAssetRepository.getOne(request.getPartnerAssetID());

        tripRequest.setBarCode(validations.generateCode(tripRequest.getReferenceNo()));
        tripRequest.setQrCode(validations.generateCode(tripRequest.getReferenceNo()));

        tripRequest.setCreatedBy(userCurrent.getId());
        tripRequest.setIsActive(true);
        tripRequest = tripRequestRepository.save(tripRequest);
        log.debug("Create new trip Request - {}"+ new Gson().toJson(tripRequest));
        TripResponseDto tripResponseDto = mapper.map(tripRequest, TripResponseDto.class);
        tripResponseDto.setPartnerName(partner.getName());
        tripResponseDto.setPartnerAssetName(partnerAsset.getName());
        return tripResponseDto;
    }

    public TripResponseDto updateTripRequest(TripRequestDto request) {
        validations.validateTripRequest(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        TripRequest tripRequest = tripRequestRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Trip Request ID does not exist!"));
        mapper.map(request, tripRequest);

        tripRequest.setUpdatedBy(userCurrent.getId());
        tripRequestRepository.save(tripRequest);
        log.debug("tripRequest record updated - {}"+ new Gson().toJson(tripRequest));
        TripResponseDto tripResponseDto = mapper.map(tripRequest, TripResponseDto.class);

        if(request.getPartnerID() != null || request.getPartnerAssetID() != null ) {
            Partner partner = partnerRepository.getOne(request.getPartnerID());
            tripResponseDto.setPartnerName(partner.getName());
            PartnerAsset partnerAsset = partnerAssetRepository.getOne(request.getPartnerAssetID());
            tripResponseDto.setPartnerAssetName(partnerAsset.getName());
        }
        return tripResponseDto;
    }

    public TripResponseDto findTripRequest(Long id){
        TripRequest tripRequest  = tripRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Trip Request Id does not exist!"));

        TripResponseDto tripResponseDto = mapper.map(tripRequest, TripResponseDto.class);
        tripResponseDto.setTripItem(getAllTripItems(id));
        tripResponseDto.setTripRequestResponse(getAllRequestResponse(id));

        return tripResponseDto;
    }


    public Page<TripRequest> findAll(Long partnerID, Long orderItemID, String status, PageRequest pageRequest ){
        GenericSpecification<TripRequest> genericSpecification = new GenericSpecification<TripRequest>();

        if (partnerID != null)
        {
            genericSpecification.add(new SearchCriteria("partnerID", partnerID, SearchOperation.EQUAL));
        }

        if (orderItemID != null)
        {
            genericSpecification.add(new SearchCriteria("orderItemID", orderItemID, SearchOperation.EQUAL));
        }

        if (status != null && !status.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("status", status, SearchOperation.EQUAL));
        }



        Page<TripRequest> tripRequests = tripRequestRepository.findAll(genericSpecification, pageRequest);
        if(tripRequests == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return tripRequests;

    }



    public void enableDisable (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        TripRequest tripRequest  = tripRequestRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Trip Request Id does not exist!"));
        tripRequest.setIsActive(request.isActive());
        tripRequest.setUpdatedBy(userCurrent.getId());
        tripRequestRepository.save(tripRequest);

    }


    public List<TripRequest> getAll(Boolean isActive){
        List<TripRequest> tripRequests = tripRequestRepository.findByIsActive(isActive);
        return tripRequests;

    }

    public List<TripItem> getAllTripItems(Long tripRequestID){
        List<TripItem> tripItems = tripItemRepository.findByTripRequestID(tripRequestID);
        return tripItems;

    }
    public List<TripRequestResponse> getAllRequestResponse(Long tripRequestID){
        List<TripRequestResponse> tripRequests = tripRequestResponseRepository.findByTripRequestID(tripRequestID);
        return tripRequests;

    }
}
