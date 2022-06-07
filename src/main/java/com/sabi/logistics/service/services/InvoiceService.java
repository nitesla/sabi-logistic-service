package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.AuditTrailService;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.AuditTrailFlag;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.framework.utils.Utility;
import com.sabi.logistics.core.dto.request.InvoiceInvoiceItemDto;
import com.sabi.logistics.core.dto.request.InvoiceRequestDto;
import com.sabi.logistics.core.dto.response.InvoiceItemResponseDto;
import com.sabi.logistics.core.dto.response.InvoiceInvoiceItemResponseDto;
import com.sabi.logistics.core.dto.response.InvoiceResponseDto;
import com.sabi.logistics.core.models.Invoice;
import com.sabi.logistics.core.models.InvoiceItem;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.InvoiceItemRepository;
import com.sabi.logistics.service.repositories.InvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final ModelMapper mapper;
    private final AuditTrailService auditTrailService;
    @Autowired
    private Validations validations;

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;
    @Autowired
    private InvoiceItemService invoiceItemService;


    public InvoiceService(InvoiceRepository invoiceRepository, ModelMapper mapper, AuditTrailService auditTrailService) {
        this.invoiceRepository = invoiceRepository;
        this.mapper = mapper;
        this.auditTrailService = auditTrailService;
    }

    public InvoiceInvoiceItemResponseDto createInvoiceAndItsItems(InvoiceInvoiceItemDto request,HttpServletRequest request1) {
        List<InvoiceItemResponseDto> responseDtos = new ArrayList<>();
        validations.validateInvoiceInvoiceItems(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Invoice invoice = mapper.map(request,Invoice.class);
        InvoiceItem invoiceItem = mapper.map(request, InvoiceItem.class);

        invoice.setReferenceNo(validations.generateReferenceNumber(10));
        Invoice invoiceExists = invoiceRepository.findByReferenceNo(invoice.getReferenceNo());
        Invoice invoiceNumberExists = invoiceRepository.findByInvoiceNumber(invoice.getInvoiceNumber());
        if(invoice.getReferenceNo() == null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Invoice does not have Reference Number");
        }
        if(invoiceExists != null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Invoice already exist");
        }
        if(invoiceNumberExists != null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Invoice Number already exist");
        }

        invoice.setBarCode(validations.generateCode(invoice.getReferenceNo()));
        invoice.setQrCode(validations.generateCode(invoice.getReferenceNo()));

        invoice.setCreatedBy(userCurrent.getId());
        invoice.setIsActive(true);
        invoice = invoiceRepository.save(invoice);
        log.debug("Create new invoice - {}"+ new Gson().toJson(invoice));
        InvoiceInvoiceItemResponseDto invoiceResponseDto = mapper.map(invoice, InvoiceInvoiceItemResponseDto.class);
        log.info("request sent ::::::::::::::::::::::::::::::::: " + request.getInvoiceItemRequestDto());
        request.getInvoiceItemRequestDto().forEach(invoiceItems ->{
            invoiceItems.setInvoiceId(invoiceResponseDto.getId());
        });
        responseDtos = invoiceItemService.createInvoiceItems(request.getInvoiceItemRequestDto(),invoice);
        //List<InvoiceItemResponseDto> finalResponseDtos = responseDtos;
//        responseDtos.forEach(invoiceItemResponseDto -> {
            invoiceResponseDto.setInvoiceItem(responseDtos);
//        });

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Create new invoice items by :" + userCurrent.getUsername(),
                        AuditTrailFlag.CREATE,
                        " Create new invoice items for:" + invoice.getCustomerName() + " "+ invoice.getReferenceNo() ,1, Utility.getClientIp(request1));
        return invoiceResponseDto;
    }

    public InvoiceResponseDto updateInvoice(InvoiceRequestDto request,HttpServletRequest request1) {
        validations.validateInvoice(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Invoice invoice = invoiceRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested invoice Id does not exist!"));
        mapper.map(request, invoice);

        invoice.setUpdatedBy(userCurrent.getId());
        invoiceRepository.save(invoice);
        log.debug("invoice record updated - {}"+ new Gson().toJson(invoice));

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Update invoice by username:" + userCurrent.getUsername(),
                        AuditTrailFlag.UPDATE,
                        " Update invoice Request for:" + invoice.getId() ,1, Utility.getClientIp(request1));
        InvoiceResponseDto invoiceResponseDto = mapper.map(invoice, InvoiceResponseDto.class);
        return invoiceResponseDto;
    }

    public InvoiceResponseDto updateInvoiceStatus(InvoiceRequestDto request) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Invoice invoice = invoiceRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested invoice Id does not exist!"));
        mapper.map(request, invoice);
        invoice.setUpdatedBy(userCurrent.getId());
        invoiceRepository.save(invoice);
        log.debug("invoice record updated - {}"+ new Gson().toJson(invoice));
        return mapper.map(invoice, InvoiceResponseDto.class);
    }

    public InvoiceResponseDto findInvoice(Long id){
        Invoice invoice  = invoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested invoice Id does not exist!"));
        InvoiceResponseDto invoiceResponseDto = mapper.map(invoice, InvoiceResponseDto.class);
        invoiceResponseDto.setInvoiceItems(getAllInvoiceItems(id));

        return invoiceResponseDto;

    }

    public InvoiceResponseDto findInvoiceNumber(String invoiceNumber){
        Invoice invoice  = invoiceRepository.findByInvoiceNumber(invoiceNumber);
        if(invoice == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        InvoiceResponseDto invoiceResponseDto = mapper.map(invoice, InvoiceResponseDto.class);
        invoiceResponseDto.setInvoiceItems(getAllInvoiceItems(invoice.getId()));

        return invoiceResponseDto;

    }

    public Page<Invoice> findAll( String referenceNo, String deliveryStatus,
                               String customerName, String customerPhone, String deliveryAddress,
                               String barCode, String qrCode, PageRequest pageRequest ){

        Page<Invoice> invoices = invoiceRepository.findInvoice(referenceNo, deliveryStatus, customerName, customerPhone,
                                                        deliveryAddress, barCode, qrCode, pageRequest);
        if(invoices == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return invoices;

    }



    public void enableDisable (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Invoice invoice  = invoiceRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Invoice Id does not exist!"));
        invoice.setIsActive(request.isActive());
        invoice.setUpdatedBy(userCurrent.getId());
        invoiceRepository.save(invoice);

    }


    public List<Invoice> getAll(Boolean isActive){
        List<Invoice> invoices = invoiceRepository.findByIsActive(isActive);
        return invoices;

    }

    public List<InvoiceItem> getAllInvoiceItems(Long invoiceId){
        List<InvoiceItem> invoiceItems = invoiceItemRepository.findByInvoiceId(invoiceId);
        return invoiceItems;

    }
}
