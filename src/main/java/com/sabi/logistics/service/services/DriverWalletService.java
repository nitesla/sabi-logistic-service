package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.AuditTrailService;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.AuditTrailFlag;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.framework.utils.Utility;
import com.sabi.logistics.core.dto.request.DriverWalletDto;
import com.sabi.logistics.core.dto.response.DriverWalletResponseDto;
import com.sabi.logistics.core.enums.TransAction;
import com.sabi.logistics.core.models.DriverWallet;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.DriverWalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

@SuppressWarnings("ALL")
@Slf4j
@Service
public class DriverWalletService {

    @Autowired
    private DriverWalletRepository repository;
    @Autowired
    private ModelMapper mapper;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private Validations validations;
    @Autowired
    private AuditTrailService auditTrailService;

    /** <summary>
     * DriverWallet creation
     * </summary>
     * <remarks>this method is responsible for creation of new DriverWallet</remarks>
     */

    public DriverWalletResponseDto createDriverWallet(DriverWalletDto request,HttpServletRequest request1) {
        validations.validateDriverWallet(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DriverWallet driverWallet = mapper.map(request,DriverWallet.class);
        DriverWallet walletExist = repository.findByDriverId(request.getDriverId());
        if(walletExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Driver wallet already exist");
        }
        log.info("wallet fetched :::::::::::::::::::::::::::::::::::::::::; " + walletExist);
        driverWallet.setPreviousAmount(BigDecimal.valueOf(0.0));
//        driverWallet.setLastTransactionDate(LocalDateTime.parse(""));
        if (request.getAction().equals(TransAction.WITHDRAWAL)){
            BigDecimal amount = request.getAmount();
            BigDecimal balance = BigDecimal.valueOf(0.0);
                  BigDecimal newBalance =  balance.subtract(request.getAmount());
            driverWallet.setBalance(newBalance);

        }
        if (request.getAction().equals(TransAction.DEPOSIT)){
            BigDecimal amount = request.getAmount();
            BigDecimal balance = BigDecimal.valueOf(0.0);
            BigDecimal newBalance =  balance.add(request.getAmount());
            driverWallet.setBalance(newBalance);

        }
//        driverWallet.setBalance(newBalance);
        driverWallet.setCreatedBy(userCurrent.getId());
        driverWallet.setIsActive(true);
        driverWallet = repository.save(driverWallet);
        log.debug("Create new Driver asset - {}"+ new Gson().toJson(driverWallet));


        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Create new driverWallet  by :" + userCurrent.getUsername(),
                        AuditTrailFlag.CREATE,
                        " Create new driverWallet for:" + driverWallet.getDriverId() ,1, Utility.getClientIp(request1));
        return mapper.map(driverWallet, DriverWalletResponseDto.class);
    }



    /** <summary>
     * DriverWallet update
     * </summary>
     * <remarks>this method is responsible for updating already existing DriverWallet</remarks>
     */

    public DriverWalletResponseDto updateDriverWallet(DriverWalletDto request,HttpServletRequest request1) {
//        validations.validateDriverWallet(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DriverWallet driverWallet = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Driver Wallet Id does not exist!"));

        mapper.map(request, driverWallet);
        driverWallet.setPreviousAmount(driverWallet.getBalance());
        driverWallet.setLastTransactionDate(driverWallet.getCreatedDate());
        /////////////////////////////////////////////////////////////////
        if (request.getAction().equals(TransAction.WITHDRAWAL)) {
            log.info("BALNCE with :::::::::::::::::::::::::::::::::::::::::::: " + driverWallet.getBalance());
            BigDecimal presentBalance = driverWallet.getBalance().subtract(request.getAmount());
            log.info("withdrawl :::::::::::::::::::::::::::::::::::::::::::: " + presentBalance);
            driverWallet.setBalance(presentBalance);
        }
        if (request.getAction().equals(TransAction.DEPOSIT)) {
            log.info("BALNCE :::::::::::::::::::::::::::::::::::::::::::: " + driverWallet.getBalance());
            BigDecimal presentBalance = driverWallet.getBalance().add(request.getAmount());
            log.info("Deposit :::::::::::::::::::::::::::::::::::::::::::: " + presentBalance);
            driverWallet.setBalance(presentBalance);
        }
//        driverWallet.setUpdatedBy(userCurrent.getId());

        /////////////////////////////////////////////////////////////////
        driverWallet.setUpdatedBy(userCurrent.getId());
        repository.save(driverWallet);
        log.debug("Driver Wallet record updated - {}" + new Gson().toJson(driverWallet));

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Update driverWallet by username:" + userCurrent.getUsername(),
                        AuditTrailFlag.UPDATE,
                        " Update driverWallet Request for:" + driverWallet.getId(),1, Utility.getClientIp(request1));
        return mapper.map(driverWallet, DriverWalletResponseDto.class);
    }





    /** <summary>
     * Find DriverWallet
     * </summary>
     * <remarks>this method is responsible for getting a single record</remarks>
     */
    public DriverWalletResponseDto findDriverWallet(Long id) {
        DriverWallet driverWallet = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested order Id does not exist!"));
        DriverWalletResponseDto orderResponseDto = mapper.map(driverWallet, DriverWalletResponseDto.class);
        return orderResponseDto;
    }



    /** <summary>
     * Find all DriverWallet
     * </summary>
     * <remarks>this method is responsible for getting all records in pagination</remarks>
     */




    public Page<DriverWallet> findDriverWallets(Long driverId, PageRequest pageRequest ) {
        Page<DriverWallet> driverWallets = repository.findDriverWallets(driverId, pageRequest);
        if (driverWallets == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return driverWallets;

    }

    public DriverWalletResponseDto prepareForFinalDriverWalletSave(DriverWallet request,HttpServletRequest request1) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DriverWallet driverWallet = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Driver Wallet Id does not exist!"));
        driverWallet.setBalance(request.getBalance());
        driverWallet.setPreviousAmount(request.getPreviousAmount());
        repository.save(driverWallet);
        log.debug("Driver Wallet record updated - {}" + new Gson().toJson(driverWallet));

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Update driverWallet by username:" + userCurrent.getUsername(),
                        AuditTrailFlag.UPDATE,
                        " Update driverWallet Request for:" + driverWallet.getId(),1, Utility.getClientIp(request1));
        return mapper.map(driverWallet, DriverWalletResponseDto.class);
    }
}
