package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.PartnerBankDto;
import com.sabi.logistics.core.dto.response.PartnerBankResponseDto;
import com.sabi.logistics.core.models.Bank;
import com.sabi.logistics.core.models.Partner;
import com.sabi.logistics.core.models.PartnerBank;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.BankRepository;
import com.sabi.logistics.service.repositories.PartnerBankRepository;
import com.sabi.logistics.service.repositories.PartnerRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@SuppressWarnings("All")
@Slf4j
@Service
public class PartnerBankService {

    private PartnerBankRepository partnerBankRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;

    @Autowired
    PartnerRepository partnerRepository;

    @Autowired
    BankRepository bankRepository;

    public PartnerBankService(PartnerBankRepository partnerBankRepository, ModelMapper mapper, ObjectMapper objectMapper, Validations validations) {
        this.partnerBankRepository = partnerBankRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
    }



    /** <summary>
     * Partner Bank creation
     * </summary>
     * <remarks>this method is responsible for creation of new partnerBank</remarks>
     */

    public PartnerBankResponseDto createPartnerBank(PartnerBankDto request) {
        validations.validatePartnerBank(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerBank partnerBank = mapper.map(request,PartnerBank.class);
        PartnerBank partnerBankExist = partnerBankRepository.findByPartnerIdAndAccountNumber(request.getPartnerId(), request.getAccountNumber());
        if(partnerBankExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Partner Bank already exist");
        }

        Partner partner = partnerRepository.findPartnerById(request.getPartnerId());
        Bank bank = bankRepository.findBankById(request.getBankId());
        partnerBank.setCreatedBy(userCurrent.getId());
        partnerBank.setIsActive(true);
        partnerBank = partnerBankRepository.save(partnerBank);
        log.debug("Create new partner bank - {}"+ new Gson().toJson(partnerBank));
        PartnerBankResponseDto partnerBankResponseDto = mapper.map(partnerBank, PartnerBankResponseDto.class);
        partnerBankResponseDto.setPartnerName(partner.getName());
        partnerBankResponseDto.setBankName(bank.getName());
        return partnerBankResponseDto;
    }



    /** <summary>
     * Partner Bank update
     * </summary>
     * <remarks>this method is responsible for updating already existing partner Bank</remarks>
     */

    public PartnerBankResponseDto updatePartnerBank(PartnerBankDto request) {
        validations.validatePartnerBank(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerBank partnerBank = partnerBankRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partnerBank id does not exist!"));
        mapper.map(request, partnerBank);
        partnerBank.setUpdatedBy(userCurrent.getId());
        partnerBankRepository.save(partnerBank);
        log.debug("PartnerBank record updated - {}"+ new Gson().toJson(partnerBank));
        PartnerBankResponseDto partnerBankResponseDto = mapper.map(partnerBank, PartnerBankResponseDto.class);
        if(request.getPartnerId() != null ) {
            Partner partner = partnerRepository.findPartnerById(request.getPartnerId());
            partnerBankResponseDto.setPartnerName(partner.getName());
        }
        if(request.getBankId() != null ) {
            Bank bank = bankRepository.findBankById(request.getBankId());
            partnerBankResponseDto.setBankName(bank.getName());
        }
        return partnerBankResponseDto;
    }

    @Transactional
    public PartnerBankResponseDto setDefalult(long id) {
        PartnerBank partnerBank = partnerBankRepository.findById(id).orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                "Requested Agent Bank does not exist!"));
        partnerBankRepository.updateIsDefault();
        partnerBank.setIsDefault(true);
        partnerBankRepository.save(partnerBank);
        PartnerBankResponseDto partnerBankResponseDto = mapper.map(partnerBank, PartnerBankResponseDto.class);
        if(partnerBank.getPartnerId() != null ) {
            Partner partner = partnerRepository.findPartnerById(partnerBank.getPartnerId());
            partnerBankResponseDto.setPartnerName(partner.getName());
        }
        if(partnerBank.getBankId() != null ) {
            Bank bank = bankRepository.findBankById(partnerBank.getBankId());
            partnerBankResponseDto.setBankName(bank.getName());
        }
        return partnerBankResponseDto;
    }



    /** <summary>
     * Find PartnerBank
     * </summary>
     * <remarks>this method is responsible for getting a single record</remarks>
     */
    public PartnerBankResponseDto findPartnerBank(Long id){
        PartnerBank partnerBank  = partnerBankRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partnerBank id does not exist!"));
        return mapper.map(partnerBank,PartnerBankResponseDto.class);
    }


    /** <summary>
     * Find all PartnerBanks
     * </summary>
     * <remarks>this method is responsible for getting all records in pagination</remarks>
     */
    public Page<PartnerBank> findAll(Long partnerId, Long bankId, String accountNumber, PageRequest pageRequest ){
        Page<PartnerBank> partnerBank = partnerBankRepository.findPartnerBanks(partnerId,bankId, accountNumber,pageRequest);
        if(partnerBank == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return partnerBank;

    }


    /** <summary>
     * Enable disenable
     * </summary>
     * <remarks>this method is responsible for enabling and dis enabling a partnerBank</remarks>
     */
    public void enableDisable (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerBank partnerBank  = partnerBankRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partnerBank Id does not exist!"));
        partnerBank.setIsActive(request.isActive());
        partnerBank.setUpdatedBy(userCurrent.getId());
        partnerBankRepository.save(partnerBank);

    }


    public List<PartnerBank> getAll(Long partnerId, Boolean  isActive){
        List<PartnerBank> partnerBanks = partnerBankRepository.findByPartnerIdAndIsActive(partnerId, isActive);

        for (PartnerBank pbank : partnerBanks) {

            Bank bank = bankRepository.findBankById(pbank.getBankId());
            if (bank == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid bankId");
            }
            pbank.setBankName(bank.getName());

        }
        return partnerBanks;

    }
}
