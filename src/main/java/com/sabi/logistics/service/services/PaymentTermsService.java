package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.PaymentTermsDto;
import com.sabi.logistics.core.dto.response.PartnerAssetTypeResponseDto;
import com.sabi.logistics.core.dto.response.PaymentTermsResponseDto;
import com.sabi.logistics.core.models.PartnerAssetType;
import com.sabi.logistics.core.models.PaymentTerms;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.AssetTypePropertiesRepository;
import com.sabi.logistics.service.repositories.PartnerAssetTypeRepository;
import com.sabi.logistics.service.repositories.PaymentTermsRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;


@SuppressWarnings("All")
@Slf4j
@Service
public class PaymentTermsService {

    private PaymentTermsRepository paymentTermsRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;

    @Autowired
    PartnerAssetTypeRepository partnerAssetTypeRepository;

    @Autowired
    private AssetTypePropertiesRepository assetTypePropertiesRepository;

    @Autowired
    private final PartnerAssetTypeService partnerAssetTypeService;


    public PaymentTermsService(PaymentTermsRepository paymentTermsRepository, ModelMapper mapper, ObjectMapper objectMapper, Validations validations, PartnerAssetTypeService partnerAssetTypeService) {
        this.paymentTermsRepository = paymentTermsRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
        this.partnerAssetTypeService = partnerAssetTypeService;
    }



    /** <summary>
     * PaymentTerms creation
     * </summary>
     * <remarks>this method is responsible for creation of new paymentTerms</remarks>
     */

    public PaymentTermsResponseDto createPaymentTerms(PaymentTermsDto request) {
        validations.validatePaymentTerms(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PaymentTerms paymentTerms = mapper.map(request,PaymentTerms.class);
        PaymentTerms paymentTermsExist = paymentTermsRepository.findByPartnerAssetTypeIdAndCreatedBy(request.getPartnerAssetTypeId(), userCurrent.getId() );
        if(paymentTermsExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Payment Terms already exist");
        }

        PartnerAssetType partnerAssetType = partnerAssetTypeRepository.getOne(request.getPartnerAssetTypeId());
        paymentTerms.setCreatedBy(userCurrent.getId());
        paymentTerms.setIsActive(true);
        paymentTerms = paymentTermsRepository.save(paymentTerms);
        log.debug("Create new partner bank - {}"+ new Gson().toJson(paymentTerms));
        PaymentTermsResponseDto paymentTermsResponseDto = mapper.map(paymentTerms, PaymentTermsResponseDto.class);
        paymentTermsResponseDto.setPartnerAssetTypeName(partnerAssetType.getAssetTypeName());
        return paymentTermsResponseDto;
    }



    /** <summary>
     * PaymentTerms update
     * </summary>
     * <remarks>this method is responsible for updating already existing PaymentTerms</remarks>
     */

    public PaymentTermsResponseDto updatePaymentTerms(PaymentTermsDto request) {
        validations.validatePaymentTerms(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PaymentTerms paymentTerms = paymentTermsRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested paymentTermsId does not exist!"));
        mapper.map(request, paymentTerms);
        paymentTerms.setUpdatedBy(userCurrent.getId());
        paymentTermsRepository.save(paymentTerms);
        log.debug("PaymentTerms record updated - {}"+ new Gson().toJson(paymentTerms));
        PaymentTermsResponseDto paymentTermsResponseDto = mapper.map(paymentTerms, PaymentTermsResponseDto.class);
        if(request.getPartnerAssetTypeId() != null ) {
            PartnerAssetType partnerAssetType = partnerAssetTypeRepository.getOne(request.getPartnerAssetTypeId());
            paymentTermsResponseDto.setPartnerAssetTypeName(partnerAssetType.getAssetTypeName());
        }
        return paymentTermsResponseDto;
    }



    /** <summary>
     * Find PaymentTerms
     * </summary>
     * <remarks>this method is responsible for getting a single record</remarks>
     */
    public PaymentTermsResponseDto findPaymentTerms(Long id){
        PaymentTerms paymentTerms  = paymentTermsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested paymentTerms id does not exist!"));
        /**
         @Description: Sets related and needed field of PaymentTerms
         @Date:       06/04/2022
         @Author:     Afam Okonkwo
         */
        paymentTerms = this.setNeededVaribles(paymentTerms);
        return mapper.map(paymentTerms,PaymentTermsResponseDto.class);
    }

    /**
     *
     * This method sets related and needed field of PaymentTerms
     * @param paymentTerms
     * @return PaymentTerms
     * @Author Afam Okonkwo
     * @Date 06/04/2022
     */
     public PaymentTerms setNeededVaribles(PaymentTerms paymentTerms){
         if (paymentTerms.getPartnerAssetTypeId()!=null){
             PartnerAssetTypeResponseDto partnerAssetTypeResponseDto = partnerAssetTypeService.findPartnerAssetType(paymentTerms.getPartnerAssetTypeId());
             if (partnerAssetTypeResponseDto!=null){
                 paymentTerms.setPartnerAssetTypeName(partnerAssetTypeResponseDto.getAssetTypeName());
                 paymentTerms.setPartnerName(partnerAssetTypeResponseDto.getPartnerName());
             }
             else {
                 throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION , "Related PartnerAssetType NotFound: Error getting paymentTerm");
             }
         }
         else {
             throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION , " The Related PartnerAssetType Id is invalid: Error getting paymentTerm");
         }
        return paymentTerms;
     }

    /** <summary>
     * Find all PaymentTermss
     * </summary>
     * <remarks>this method is responsible for getting all records in pagination</remarks>
     */
    public Page<PaymentTerms> findAll(Long partnerAssetTypeId, Integer days, Boolean isActive,Long partnerId,PageRequest pageRequest ){
        Page<PaymentTerms> paymentTerms = paymentTermsRepository.findPaymentTerms(partnerAssetTypeId,days,isActive,partnerId, pageRequest);
        if(paymentTerms == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        /**
         @Description: Sets related and needed field of PaymentTerms
         @Date:       06/04/2022
         @Author:     Afam Okonkwo
         */
        paymentTerms.getContent().stream().forEach(this::setNeededVaribles);
        return paymentTerms;

    }


    /** <summary>
     * Enable disenable
     * </summary>
     * <remarks>this method is responsible for enabling and dis enabling a paymentTerms</remarks>
     */
    public void enableDisable (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PaymentTerms paymentTerms  = paymentTermsRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested paymentTerms Id does not exist!"));
        paymentTerms.setIsActive(request.getIsActive());
        paymentTerms.setUpdatedBy(userCurrent.getId());
        paymentTermsRepository.save(paymentTerms);

    }


    public List<PaymentTerms> getAll(Long partnerId, Boolean isActive){
        List<PaymentTerms> paymentTerms = paymentTermsRepository.findByPartnerIdAndIsActive(partnerId, isActive);
        /**
         @Description: Sets related and needed field of PaymentTerms
         @Date:       6/04/2022
         @Author:     Afam Okonkwo
         */
        paymentTerms.stream().forEach(this::setNeededVaribles);
        return paymentTerms;
    }
}
