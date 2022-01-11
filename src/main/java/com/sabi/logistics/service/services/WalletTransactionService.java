package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.DriverWalletDto;
import com.sabi.logistics.core.dto.request.WalletTransactionDto;
import com.sabi.logistics.core.dto.response.WalletTransactionResponseDto;
import com.sabi.logistics.core.enums.TransAction;
import com.sabi.logistics.core.models.DriverWallet;
import com.sabi.logistics.core.models.WalletTransaction;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.DriverWalletRepository;
import com.sabi.logistics.service.repositories.TransactionWalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@SuppressWarnings("All")
@Service
@Slf4j
public class WalletTransactionService {

    @Autowired
    private TransactionWalletRepository repository;
    @Autowired
    private DriverWalletRepository driverWalletRepository;
    @Autowired
    private DriverWalletService driverWalletService;
    @Autowired
    private ModelMapper mapper;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private Validations validations;

    /** <summary>
     * WalletTransaction creation
     * </summary>
     * <remarks>this method is responsible for creation of new WalletTransaction</remarks>
     */

    public WalletTransactionResponseDto createWalletTransaction(WalletTransactionDto request,HttpServletRequest request1) {
        validations.validateWalletTransaction(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        WalletTransaction walletTransaction = mapper.map(request,WalletTransaction.class);
        List<WalletTransaction> savedDriverWallet = repository.findByDriverWalletId(request.getDriverWalletId());
        DriverWallet driverWallet = driverWalletRepository.getOne(request.getDriverWalletId());
        walletTransaction.setPreviousBalance(driverWallet.getBalance());
        if (request.getAction().equals(TransAction.DEPOSIT)) {
            walletTransaction.setBalance(driverWallet.getBalance().add(request.getAmount()));
        }
        if (request.getAction().equals(TransAction.WITHDRAWAL)){
            walletTransaction.setBalance(driverWallet.getBalance().subtract (request.getAmount()));
        }
        walletTransaction.setActionDate(LocalDateTime.now());
        walletTransaction.setCreatedBy(userCurrent.getId());
        walletTransaction.setIsActive(true);
        walletTransaction = repository.save(walletTransaction);
        DriverWalletDto driverWalletDto = new DriverWalletDto();
        driverWalletDto.setAction(walletTransaction.getAction());
        driverWalletDto.setAmount(walletTransaction.getAmount());
        driverWalletDto.setId(walletTransaction.getDriverWalletId());
        driverWalletService.updateDriverWallet(driverWalletDto,request1);

        log.debug("Create new Driver wallet - {}"+ new Gson().toJson(walletTransaction));
        return mapper.map(walletTransaction, WalletTransactionResponseDto.class);
    }



//    /** <summary>
//     * WalletTransaction update
//     * </summary>
//     * <remarks>this method is responsible for updating already existing WalletTransaction</remarks>
//     */
//
//    public WalletTransactionResponseDto updateWalletTransaction(WalletTransactionDto request) {
//        validations.validateWalletTransaction(request);
//        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
//        WalletTransaction driverWallet = repository.findById(request.getId())
//                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
//                        "Requested Wallet transaction Id does not exist!"));
//
//        mapper.map(request, driverWallet);
//        driverWallet.setPreviousBalance(driverWallet.getBalance());
//        /////////////////////////////////////////////////////////////////
//        if (request.getAction().equals(TransAction.WITHDRAWAL)) {
//            log.info("BALNCE with :::::::::::::::::::::::::::::::::::::::::::: " + driverWallet.getBalance());
//            BigDecimal presentBalance = driverWallet.getBalance().subtract(request.getAmount());
//            log.info("withdrawl :::::::::::::::::::::::::::::::::::::::::::: " + presentBalance);
//            driverWallet.setBalance(presentBalance);
//        }
//        if (request.getAction().equals(TransAction.DEPOSIT)) {
//            log.info("BALNCE :::::::::::::::::::::::::::::::::::::::::::: " + driverWallet.getBalance());
//            BigDecimal presentBalance = driverWallet.getBalance().add(request.getAmount());
//            log.info("Deposit :::::::::::::::::::::::::::::::::::::::::::: " + presentBalance);
//            driverWallet.setBalance(presentBalance);
//        }
////        driverWallet.setUpdatedBy(userCurrent.getId());
//
//        /////////////////////////////////////////////////////////////////
//        driverWallet.setUpdatedBy(userCurrent.getId());
//        repository.save(driverWallet);
//        log.debug("Wallet transaction record updated - {}" + new Gson().toJson(driverWallet));
//        return mapper.map(driverWallet, WalletTransactionResponseDto.class);
//    }





    /** <summary>
     * Find DriverWallet
     * </summary>
     * <remarks>this method is responsible for getting a single record</remarks>
     */
    public WalletTransactionResponseDto findWalletTransaction(Long id) {
        WalletTransaction walletTransaction = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested wallet transaction Id does not exist!"));
        WalletTransactionResponseDto orderResponseDto = mapper.map(walletTransaction, WalletTransactionResponseDto.class);
        return orderResponseDto;
    }



    /** <summary>
     * Find all WalletTransaction
     * </summary>
     * <remarks>this method is responsible for getting all records in pagination</remarks>
     */




    public Page<WalletTransaction> findWalletTransactions(Long driverWalletId,Long dropOffId, PageRequest pageRequest ) {
        Page<WalletTransaction> walletTransactions = repository.findWalletTransactions(driverWalletId,dropOffId, pageRequest);
        if (walletTransactions == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return walletTransactions;

    }

    public WalletTransactionResponseDto prepareForFinalWalletTRansactionSave(WalletTransaction request) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        WalletTransaction walletTransaction = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Wallet transaction Id does not exist!"));
        walletTransaction.setBalance(request.getBalance());
        repository.save(walletTransaction);
        log.debug("Wallet transaction record updated - {}" + new Gson().toJson(walletTransaction));
        return mapper.map(walletTransaction, WalletTransactionResponseDto.class);
    }
}
