package com.sabi.logistics.service.services;


import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.DropOffInvoicePaymentRequestDto;
import com.sabi.logistics.core.dto.response.InvoicePaymentResponseDto;
import com.sabi.logistics.core.models.DropOffInvoicePayment;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.DropOffInvoicePaymentRepository;
import com.sabi.logistics.service.repositories.InvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DropOffInvoicePaymentService {

    private final ModelMapper modelMapper;

    private final DropOffInvoicePaymentRepository dropOffInvoicePaymentRepository;

    private final Validations validations;

    private final InvoiceRepository invoiceRepository;

    public DropOffInvoicePaymentService(ModelMapper modelMapper, DropOffInvoicePaymentRepository dropOffInvoicePaymentRepository, Validations validations, InvoiceRepository invoiceRepository) {
        this.modelMapper = modelMapper;
        this.dropOffInvoicePaymentRepository = dropOffInvoicePaymentRepository;
        this.validations = validations;
        this.invoiceRepository = invoiceRepository;
    }

    public InvoicePaymentResponseDto createDropOffInvoicePayment(DropOffInvoicePaymentRequestDto dropOffInvoicePaymentRequest) {
        this.validations.validateDropOffPaymentInvoice(dropOffInvoicePaymentRequest);
        User currentUser = TokenService.getCurrentUserFromSecurityContext();
        DropOffInvoicePayment dropOffInvoicePayment = dropOffInvoicePaymentRepository.findByDropOffInvoiceIdAndInvoicePaymentId(dropOffInvoicePaymentRequest.getDropOffInvoiceId(), dropOffInvoicePaymentRequest.getInvoicePaymentId());
        if (dropOffInvoicePayment != null)
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"This dropOffInvoicePayment already exists");
        dropOffInvoicePayment = modelMapper.map(dropOffInvoicePaymentRequest,DropOffInvoicePayment.class);
        dropOffInvoicePayment.setCreatedBy(currentUser.getId());
        dropOffInvoicePayment.setIsActive(true);
        dropOffInvoicePayment = dropOffInvoicePaymentRepository.save(dropOffInvoicePayment);
        log.info("Successfully created an DropOffInvoicePayment -> {}",dropOffInvoicePayment);
        return modelMapper.map(dropOffInvoicePayment, InvoicePaymentResponseDto.class);
    }




}