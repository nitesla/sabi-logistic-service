package com.sabi.logistics.service.services;

import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.InvoicePaymentRequestDto;
import com.sabi.logistics.core.dto.response.InvoicePaymentResponseDto;
import com.sabi.logistics.core.enums.PaymentChannel;
import com.sabi.logistics.core.models.InvoicePayment;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.InvoicePaymentRepository;
import com.sabi.logistics.service.repositories.InvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class InvoicePaymentService {

    private final ModelMapper modelMapper;

    private final InvoicePaymentRepository invoicePaymentRepository;

    private final Validations validations;

    private final InvoiceRepository invoiceRepository;

    public InvoicePaymentService(ModelMapper modelMapper, InvoicePaymentRepository invoicePaymentRepository, Validations validations, InvoiceRepository invoiceRepository) {
        this.modelMapper = modelMapper;
        this.invoicePaymentRepository = invoicePaymentRepository;
        this.validations = validations;
        this.invoiceRepository = invoiceRepository;
    }

    public InvoicePaymentResponseDto createInvoicePayment(InvoicePaymentRequestDto invoicePaymentRequestDto) {
        this.validations.validateInvoicePayment(invoicePaymentRequestDto);
        User currentUser = TokenService.getCurrentUserFromSecurityContext();
        InvoicePayment invoicePayment = invoicePaymentRepository.findByPaymentReferenceAndInvoiceId(invoicePaymentRequestDto.getPaymentReference(), invoicePaymentRequestDto.getInvoiceId());
        if (invoicePayment != null)
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"This invoicePayment already exists");
        invoicePayment = modelMapper.map(invoicePaymentRequestDto,InvoicePayment.class);
        invoicePayment.setCreatedBy(currentUser.getId());
        invoicePayment.setIsActive(true);
        invoicePayment = invoicePaymentRepository.save(invoicePayment);
        log.info("Successfully created an InvoicePayment -> {}",invoicePayment);
        return modelMapper.map(invoicePayment, InvoicePaymentResponseDto.class);
    }

    public InvoicePaymentResponseDto updateInvoicePayment(InvoicePaymentRequestDto invoicePaymentRequestDto){
        this.validations.validateInvoicePayment(invoicePaymentRequestDto);
        User currentUser = TokenService.getCurrentUserFromSecurityContext();
        InvoicePayment invoicePayment = invoicePaymentRepository.findById(invoicePaymentRequestDto.getId())
                .orElseThrow(() -> new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The requested invoicePayment Id doesn't exist"));
        modelMapper.map(invoicePaymentRequestDto, invoicePayment);
        invoicePayment.setUpdatedBy(currentUser.getId());
        invoicePayment = invoicePaymentRepository.save(invoicePayment);
        invoicePayment = this.setInvoiceInformation(invoicePayment);
        log.info("Successfully updated invoicePayment->",invoicePayment);
        return modelMapper.map(invoicePayment, InvoicePaymentResponseDto.class);

    }

    public InvoicePaymentResponseDto findInvoicePayment(Long id) {
        InvoicePayment invoicePayment = invoicePaymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The requested invoicePaymentId does not exist"));
        invoicePayment = this.setInvoiceInformation(invoicePayment);
        return modelMapper.map(invoicePayment, InvoicePaymentResponseDto.class);
    }

    public List<InvoicePayment> findByIsActive(Boolean isActive) {
        List<InvoicePayment> invoicePayments = invoicePaymentRepository.findByIsActive(isActive);

        invoicePayments.forEach(this::setInvoiceInformation);

        return invoicePayments;
    }


    public Page<InvoicePayment> searchInvoicePayment(String paymentChannel, BigDecimal totalAmount, BigDecimal amountCollected, BigDecimal balanceBefore,
                                                     BigDecimal balanceAfter, Long invoiceId, String paymentReference, Boolean isActive, Pageable pageable) {
        if (paymentChannel != null)
            paymentChannel = paymentChannel.toUpperCase();// since the enums are stored in uppercase
        Page<InvoicePayment> invoicePaymentPage = invoicePaymentRepository.searchInvoicePayments(paymentChannel,totalAmount,amountCollected,balanceBefore,balanceAfter,invoiceId,paymentReference,isActive, pageable);
        invoicePaymentPage.getContent().forEach(this::setInvoiceInformation);
        return invoicePaymentPage;
    }

    public InvoicePaymentResponseDto findByPaymentReference(String paymentReference) {
        InvoicePayment invoicePayment = invoicePaymentRepository.findByPaymentReference(paymentReference);
        if (invoicePayment == null)
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "This paymentReference is not found");
        invoicePayment = this.setInvoiceInformation(invoicePayment);
        return modelMapper.map(invoicePayment,InvoicePaymentResponseDto.class);
    }

    private InvoicePayment setInvoiceInformation(InvoicePayment invoicePayment) {
        invoicePayment.setInvoice(invoiceRepository.findInvoiceById(invoicePayment.getInvoiceId()));
        return invoicePayment;
    }

    public void enableDisable(EnableDisEnableDto enableDisEnableDto) {
        InvoicePayment invoicePayment = invoicePaymentRepository.findById(enableDisEnableDto.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "The id of the invoicePayment does not exist"));
        invoicePayment.setIsActive(enableDisEnableDto.isActive());
        invoicePayment = invoicePaymentRepository.save(invoicePayment);
    }
}