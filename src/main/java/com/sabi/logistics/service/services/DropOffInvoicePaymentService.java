package com.sabi.logistics.service.services;

import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.BadRequestException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.DropOffInvoicePaymentRequestDto;
import com.sabi.logistics.core.dto.response.DropOffInvoicePaymentResponseDto;
import com.sabi.logistics.core.enums.PaymentChannel;
import com.sabi.logistics.core.enums.SlaName;
import com.sabi.logistics.core.models.DropOffInvoicePayment;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.DropOffInvoicePaymentRepository;
import com.sabi.logistics.service.repositories.DropOffInvoiceRepository;
import com.sabi.logistics.service.repositories.InvoicePaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DropOffInvoicePaymentService {

    private final DropOffInvoicePaymentRepository dropOffInvoicePaymentRepository;

    private final Validations validations;

    private final ModelMapper mapper;

    private final DropOffInvoiceRepository dropOffInvoiceRepository;

    private final InvoicePaymentRepository invoicePaymentRepository;


    public DropOffInvoicePaymentService(DropOffInvoicePaymentRepository dropOffInvoicePaymentRepository, Validations validations, ModelMapper mapper, DropOffInvoiceRepository dropOffInvoiceRepository, InvoicePaymentRepository invoicePaymentRepository) {
        this.dropOffInvoicePaymentRepository = dropOffInvoicePaymentRepository;
        this.validations = validations;
        this.mapper = mapper;
        this.dropOffInvoiceRepository = dropOffInvoiceRepository;

        this.invoicePaymentRepository = invoicePaymentRepository;
    }

    public DropOffInvoicePaymentResponseDto createDropOffInvoicePayment(DropOffInvoicePaymentRequestDto dropOffInvoicePaymentRequestDto) {
        this.validations.validateDropOffInvoicePayment(dropOffInvoicePaymentRequestDto);

        DropOffInvoicePayment dropOffInvoicePayment = dropOffInvoicePaymentRepository.findByDropOffInvoiceIdAndInvoicePaymentId(dropOffInvoicePaymentRequestDto.getDropOffInvoiceId(),dropOffInvoicePaymentRequestDto.getInvoicePaymentId());
        if (dropOffInvoicePayment != null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST,"Ooops!, this DropOffInvoicePayment already exists");

        User currentUser = TokenService.getCurrentUserFromSecurityContext();

        dropOffInvoicePayment =  mapper.map(dropOffInvoicePaymentRequestDto, DropOffInvoicePayment.class);
        dropOffInvoicePayment.setCreatedBy(currentUser.getId());
        dropOffInvoicePayment.setIsActive(true);

        dropOffInvoicePayment = dropOffInvoicePaymentRepository.save(dropOffInvoicePayment);

        log.info("Successfully created  dropOffInvoicePayment -> {}",dropOffInvoicePayment);

        return mapper.map(dropOffInvoicePayment,DropOffInvoicePaymentResponseDto.class);
    }

    public DropOffInvoicePaymentResponseDto updateDropOffInvoicePayment(DropOffInvoicePaymentRequestDto dropOffInvoicePaymentRequestDto){
        this.validations.validateDropOffInvoicePayment(dropOffInvoicePaymentRequestDto);

        DropOffInvoicePayment dropOffInvoicePayment = dropOffInvoicePaymentRepository.findById(dropOffInvoicePaymentRequestDto.getId())
                .orElseThrow(()-> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The submitted DropOffInvoicePayment id doesn't exist"));

        User currentUser = TokenService.getCurrentUserFromSecurityContext();

        mapper.map(dropOffInvoicePaymentRequestDto, dropOffInvoicePayment);
        dropOffInvoicePayment.setUpdatedBy(currentUser.getId());
        dropOffInvoicePayment = dropOffInvoicePaymentRepository.save(dropOffInvoicePayment);
        log.info("Successfully updated dropOffInvoicePayment -> {}", dropOffInvoicePayment);

        return mapper.map(this.setInvoicePaymentAndDropOffInvoiceDetails(dropOffInvoicePayment), DropOffInvoicePaymentResponseDto.class);
    }

    public DropOffInvoicePaymentResponseDto findSingleDropOffInvoicePayment(Long id) {
        DropOffInvoicePayment dropOffInvoicePayment = dropOffInvoicePaymentRepository.findById(id)
                .orElseThrow(()->new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The requested dropOffInvoicePayment id does not exist"));
        return mapper.map(this.setInvoicePaymentAndDropOffInvoiceDetails(dropOffInvoicePayment), DropOffInvoicePaymentResponseDto.class);
    }


    public List<DropOffInvoicePayment> findAllDropOffInvoicePaymentByIsActive(Boolean isActive) {
        List<DropOffInvoicePayment> byIsActive = dropOffInvoicePaymentRepository.findByIsActive(isActive);
        byIsActive.forEach(this::setInvoicePaymentAndDropOffInvoiceDetails);
        return byIsActive;
    }

    public Page<DropOffInvoicePayment> searchAllDropOffInvoicePayment(String invoiceReferenceNo, PaymentChannel paymentChannel,
                                                                      String deliveryStatus,String paymentReference,
                                                                      String customerName,
                                                                      String customerPhone, Boolean isActive, Pageable pageable) {
        Page<DropOffInvoicePayment> dropOffInvoicePayments = dropOffInvoicePaymentRepository
                .searchAll(invoiceReferenceNo, paymentChannel, deliveryStatus,
                        paymentReference, customerName, customerPhone, isActive, pageable);
        dropOffInvoicePayments.getContent().forEach(this::setInvoicePaymentAndDropOffInvoiceDetails);

        return dropOffInvoicePayments;
    }

    public DropOffInvoicePayment setInvoicePaymentAndDropOffInvoiceDetails(DropOffInvoicePayment dropOffInvoicePayment){
        dropOffInvoicePayment.setDropOffInvoice(dropOffInvoiceRepository.getOne(dropOffInvoicePayment.getDropOffInvoiceId()));
        dropOffInvoicePayment.setInvoicePayment(invoicePaymentRepository.getOne(dropOffInvoicePayment.getInvoicePaymentId()));
        return dropOffInvoicePayment;
    }

    public void enableDisable(EnableDisEnableDto enableDisEnableDto) {
        DropOffInvoicePayment dropOffInvoicePayment = dropOffInvoicePaymentRepository.findById(enableDisEnableDto.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "The DropOffInvoicePayment with the given Id cannot be found"));
        dropOffInvoicePayment.setIsActive(enableDisEnableDto.isActive());
        dropOffInvoicePayment = dropOffInvoicePaymentRepository.save(dropOffInvoicePayment);
    }
}
