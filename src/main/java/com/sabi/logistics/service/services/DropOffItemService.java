package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.BadRequestException;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.service.AuditTrailService;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.AuditTrailFlag;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.framework.utils.Utility;
import com.sabi.logistics.core.dto.request.DropOffInvoiceRequestDto;
import com.sabi.logistics.core.dto.request.DropOffItemRequestDto;
import com.sabi.logistics.core.dto.request.TripItemRequestDto;
import com.sabi.logistics.core.dto.response.DropOffInvoiceResponseDto;
import com.sabi.logistics.core.dto.response.DropOffItemResponseDto;
import com.sabi.logistics.core.enums.PaymentChannel;
import com.sabi.logistics.core.enums.PaymentStatus;
import com.sabi.logistics.core.enums.VerificationStatus;
import com.sabi.logistics.core.models.*;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@SuppressWarnings("All")
@Service
@Slf4j
public class DropOffItemService {
    private final DropOffItemRepository dropOffItemRepository;
    private final ModelMapper mapper;
    @Autowired
    private Validations validations;

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private TripRequestRepository tripRequestRepository;

    @Autowired
    private TripItemRepository tripItemRepository;

    @Autowired
    private DropOffRepository dropOffRepository;

    @Autowired
    private DropOffInvoiceRepository dropOffInvoiceRepository;

    @Autowired
    private DropOffInvoiceService dropOffInvoiceService;

    @Autowired
    private TripItemService tripItemService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private UserRepository userRepository;


    public DropOffItemService(DropOffItemRepository dropOffItemRepository, ModelMapper mapper) {
        this.dropOffItemRepository = dropOffItemRepository;
        this.mapper = mapper;
    }

    public DropOffItemResponseDto createDropOffItem(DropOffItemRequestDto request,HttpServletRequest request1) {
        validations.validateDropOffItem(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DropOffItem dropOffItem = mapper.map(request,DropOffItem.class);
        DropOffItem dropOffItemExists = dropOffItemRepository.findByInvoiceItemIdAndDropOffId(request.getInvoiceItemId(), request.getDropOffId());
        if(dropOffItemExists !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " DropOff Item already exist");
        }
        if (!(request.getStatus().equalsIgnoreCase("completed") || request.getStatus().equalsIgnoreCase("cancelled"))) {
            DropOffItem dropOffItemm = dropOffItemRepository.findByInvoiceItemIdAndStatus(request.getInvoiceItemId(), request.getStatus());
            if (dropOffItemm != null) {
                throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " DropOff Item already exist");
            }
        }
        InvoiceItem invoiceItem = invoiceItemRepository.getOne(request.getInvoiceItemId());
        Invoice invoice = invoiceRepository.getOne(invoiceItem.getInvoiceId());
        DropOff dropOff = dropOffRepository.getOne(request.getDropOffId());
        dropOffItem.setCreatedBy(userCurrent.getId());
        dropOffItem.setIsActive(true);
        dropOffItem.setFinalDropOff(false);
        dropOffItem.setProductName(invoiceItem.getProductName());
        dropOffItem.setQty(invoiceItem.getQty());
        dropOffItem.setCustomerName(invoice.getCustomerName());
        dropOffItem.setCustomerPhone(invoice.getCustomerPhone());
        if(invoiceItem.getDeliveryAddress() != null){
        dropOffItem.setDeliveryAddress(invoiceItem.getDeliveryAddress());
        }
        dropOffItem.setUnitPrice(invoiceItem.getUnitPrice());
        if (invoice.getPaymentStatus() == PaymentStatus.paid){
            dropOffItem.setTransactionReference(invoiceItem.getPaymentReference());
        }
        dropOffItem.setTotalAmount((dropOffItem.getUnitPrice().multiply(new BigDecimal(invoiceItem.getQty()))));
        if(request.getAmountCollected()!=null){
            dropOffItem.setOutstandingAmount(dropOffItem.getTotalAmount().subtract(request.getAmountCollected()));
        }
        dropOffItem = dropOffItemRepository.save(dropOffItem);
        log.debug("Create new trip item - {}"+ new Gson().toJson(dropOffItem));
        DropOffItemResponseDto dropOffItemResponseDto = mapper.map(dropOffItem, DropOffItemResponseDto.class);
        dropOffItemResponseDto.setInvoiceItemName(invoiceItem.getProductName());
        dropOffItemResponseDto.setThirdPartyProductId(invoiceItem.getThirdPartyProductId());
        dropOffItemResponseDto.setCustomerName(invoice.getCustomerName());
        dropOffItemResponseDto.setCustomerPhone(invoice.getCustomerPhone());
        dropOffItemResponseDto.setInvoiceId(invoiceItem.getInvoiceId());
        dropOffItemResponseDto.setDropOffId(dropOffItem.getDropOffId());
        dropOffItemResponseDto.setInvoiceItemId(dropOffItem.getInvoiceItemId());

        List<DropOffItem> dropOffItems = new ArrayList<>();
        List<InvoiceItem> invoiceItems = new ArrayList<>();
        TripItem tripItem = new TripItem();
        TripItemRequestDto tripItemRequestDto = new TripItemRequestDto();
        DropOffInvoice dropOffInvoice = new DropOffInvoice();
        DropOffInvoiceRequestDto dropOffInvoiceRequestDto = new DropOffInvoiceRequestDto();
        DropOffInvoiceResponseDto dropOffInvoiceResponseDto = new DropOffInvoiceResponseDto();

        tripItem = tripItemRepository.findByTripRequestIdAndThirdPartyProductId(dropOff.getTripRequestId(), invoiceItem.getThirdPartyProductId());
        dropOffItems = dropOffItemRepository.findByTripRequestIdAndThirdPartyProductId(dropOff.getTripRequestId(), invoiceItem.getThirdPartyProductId());
        invoiceItems = invoiceItemRepository.findByInvoiceIdAndThirdPartyProductId(dropOff.getDropOffInvoice().get(0).getInvoiceId(), invoiceItem.getThirdPartyProductId());
        dropOffInvoice = dropOffInvoiceRepository.findByDropOffIdAndInvoiceId(dropOff.getId(), invoice.getId());

        if(tripItem == null) {
            tripItemRequestDto.setTripRequestId(dropOff.getTripRequestId());
            tripItemRequestDto.setThirdPartyProductId(invoiceItem.getThirdPartyProductId());
            tripItemRequestDto.setProductName(invoiceItem.getProductName());
            tripItemRequestDto.setQty(getQty(invoiceItems));
            tripItemRequestDto.setQtyPickedUp(getQtyPickedUp(dropOffItems));
            tripItemService.createTripItem(tripItemRequestDto);
        } else {
            tripItemRequestDto.setTripRequestId(dropOff.getTripRequestId());
            tripItemRequestDto.setThirdPartyProductId(invoiceItem.getThirdPartyProductId());
            tripItemRequestDto.setProductName(invoiceItem.getProductName());
            tripItemRequestDto.setQty(getQty(invoiceItems));
            tripItemRequestDto.setQtyPickedUp(getQtyPickedUp(dropOffItems));
            tripItemRequestDto.setId(tripItem.getId());
            tripItemService.updateTripItem(tripItemRequestDto);
        }

        if(dropOffInvoice == null) {
            dropOffInvoiceRequestDto.setInvoiceId(invoice.getId());
            dropOffInvoiceRequestDto.setDropOffId(dropOff.getId());
            dropOffInvoiceRequestDto.setAmount(getTotalAmount(dropOffItems));
            dropOffInvoiceRequestDto.setAmountCollected(getTotalAmountCollected(dropOffItems));
            dropOffInvoiceRequestDto.setStatus(dropOffItem.getStatus());
            dropOffInvoiceResponseDto = dropOffInvoiceService.createDropOffInvoice(dropOffInvoiceRequestDto);
        } else {
            dropOffInvoiceRequestDto.setInvoiceId(invoice.getId());
            dropOffInvoiceRequestDto.setDropOffId(dropOff.getId());
            dropOffInvoiceRequestDto.setAmount(getTotalAmount(dropOffItems));
            dropOffInvoiceRequestDto.setAmountCollected(getTotalAmountCollected(dropOffItems));
            dropOffInvoiceRequestDto.setStatus(dropOffItem.getStatus());
            dropOffInvoiceRequestDto.setId(dropOffInvoice.getId());
            dropOffInvoiceResponseDto = dropOffInvoiceService.updateDropOffInvoice(dropOffInvoiceRequestDto);
        }

        dropOffItemResponseDto.setDropOffInvoiceId(dropOffInvoiceResponseDto.getId());

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Create new dropOffItem  by :" + userCurrent.getUsername(),
                        AuditTrailFlag.CREATE,
                        " Create new dropOffItem for:" + dropOffItem.getCustomerName() ,1, Utility.getClientIp(request1));

        return dropOffItemResponseDto;
    }

    @Transactional
    public List<DropOffItem> createDropOffItems(List<DropOffItemRequestDto> requests, Long dropOffId) {
        List<DropOffItem> responseDtos = new ArrayList<>();
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        requests.forEach(request-> {
            request.setDropOffId(dropOffId);
            validations.validateDropOffItem(request);
            DropOffItem dropOffItemExists = dropOffItemRepository.findByInvoiceItemIdAndDropOffId(request.getInvoiceItemId(), request.getDropOffId());
            if(dropOffItemExists !=null){
                throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " DropOff Item already exist");
            }

            if (!(request.getStatus().equalsIgnoreCase("completed") || request.getStatus().equalsIgnoreCase("cancelled"))) {
                DropOffItem dropOffItemm = dropOffItemRepository.findByInvoiceItemIdAndStatus(request.getInvoiceItemId(), request.getStatus());
                if (dropOffItemm != null) {
                    throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " DropOff Item already exist");
                }
            }

            DropOffItem dropOffItem = mapper.map(request, DropOffItem.class);
            InvoiceItem invoiceItem = invoiceItemRepository.getOne(request.getInvoiceItemId());
            Invoice invoice = invoiceRepository.getOne(invoiceItem.getInvoiceId());
            DropOff dropOff = dropOffRepository.getOne(request.getDropOffId());
            dropOffItem.setCreatedBy(userCurrent.getId());
            dropOffItem.setIsActive(true);
            dropOffItem.setFinalDropOff(false);
            dropOffItem.setQty(invoiceItem.getQty());
            dropOffItem.setCustomerName(invoice.getCustomerName());
            dropOffItem.setCustomerPhone(invoice.getCustomerPhone());
            if(invoiceItem.getDeliveryAddress() != null){
                dropOffItem.setDeliveryAddress(invoiceItem.getDeliveryAddress());
            }
            dropOffItem.setProductName(invoiceItem.getProductName());
            dropOffItem.setUnitPrice(invoiceItem.getUnitPrice());
            if (invoice.getPaymentStatus() == PaymentStatus.paid){
                dropOffItem.setTransactionReference(invoiceItem.getPaymentReference());
            }
            dropOffItem.setTotalAmount(dropOffItem.getUnitPrice().multiply(new BigDecimal(invoiceItem.getQty())));
            if(request.getAmountCollected()!=null){
                dropOffItem.setOutstandingAmount(dropOffItem.getTotalAmount().subtract(request.getAmountCollected()));
            }
            dropOffItem = dropOffItemRepository.save(dropOffItem);
            log.debug("Create new trip item - {}" + new Gson().toJson(dropOffItem));
            DropOffItem dropOffItemResponseDto = mapper.map(dropOffItem, DropOffItem.class);
            dropOffItemResponseDto.setInvoiceItemName(invoiceItem.getProductName());
            dropOffItemResponseDto.setThirdPartyProductId(invoiceItem.getThirdPartyProductId());
            dropOffItemResponseDto.setInvoiceId(invoiceItem.getInvoiceId());
            dropOffItemResponseDto.setDropOffId(dropOffItem.getDropOffId());
            dropOffItemResponseDto.setInvoiceItemId(dropOffItem.getInvoiceItemId());

            invoiceItem.setDeliveryStatus("AwaitingDelivery");
            invoiceItemRepository.save(invoiceItem);

            List<DropOffItem> dropOffItems = new ArrayList<>();
            List<InvoiceItem> invoiceItems = new ArrayList<>();
            TripItem tripItem = new TripItem();
            TripItemRequestDto tripItemRequestDto = new TripItemRequestDto();
            DropOffInvoice dropOffInvoice = new DropOffInvoice();
            DropOffInvoiceRequestDto dropOffInvoiceRequestDto = new DropOffInvoiceRequestDto();
            DropOffInvoiceResponseDto dropOffInvoiceResponseDto = new DropOffInvoiceResponseDto();

            tripItem = tripItemRepository.findByTripRequestIdAndThirdPartyProductId(dropOff.getTripRequestId(), invoiceItem.getThirdPartyProductId());
            dropOffItems = dropOffItemRepository.findByTripRequestIdAndThirdPartyProductId(dropOff.getTripRequestId(), invoiceItem.getThirdPartyProductId());
            invoiceItems = invoiceItemRepository.findByInvoiceIdAndThirdPartyProductId(dropOff.getDropOffInvoice().get(0).getInvoiceId(), invoiceItem.getThirdPartyProductId());
            dropOffInvoice = dropOffInvoiceRepository.findByDropOffIdAndInvoiceId(dropOff.getId(), invoice.getId());

            if (tripItem == null) {
                tripItemRequestDto.setTripRequestId(dropOff.getTripRequestId());
                tripItemRequestDto.setThirdPartyProductId(invoiceItem.getThirdPartyProductId());
                tripItemRequestDto.setProductName(invoiceItem.getProductName());
                tripItemRequestDto.setQty(getQty(invoiceItems));
                tripItemRequestDto.setQtyPickedUp(getQtyPickedUp(dropOffItems));
                tripItemService.createTripItem(tripItemRequestDto);
            } else {
                tripItemRequestDto.setTripRequestId(dropOff.getTripRequestId());
                tripItemRequestDto.setThirdPartyProductId(invoiceItem.getThirdPartyProductId());
                tripItemRequestDto.setProductName(invoiceItem.getProductName());
                tripItemRequestDto.setQty(getQty(invoiceItems));
                tripItemRequestDto.setQtyPickedUp(getQtyPickedUp(dropOffItems));
                tripItemRequestDto.setId(tripItem.getId());
                tripItemService.updateTripItem(tripItemRequestDto);
            }

            if(dropOffInvoice == null) {
                dropOffInvoiceRequestDto.setInvoiceId(invoice.getId());
                dropOffInvoiceRequestDto.setDropOffId(dropOff.getId());
                dropOffInvoiceRequestDto.setAmount(getTotalAmount(dropOffItems));
                dropOffInvoiceRequestDto.setAmountCollected(getTotalAmountCollected(dropOffItems));
                dropOffInvoiceRequestDto.setStatus(dropOffItem.getStatus());
                dropOffInvoiceResponseDto = dropOffInvoiceService.createDropOffInvoice(dropOffInvoiceRequestDto);
            } else {
                dropOffInvoiceRequestDto.setInvoiceId(invoice.getId());
                dropOffInvoiceRequestDto.setDropOffId(dropOff.getId());
                dropOffInvoiceRequestDto.setAmount(getTotalAmount(dropOffItems));
                dropOffInvoiceRequestDto.setAmountCollected(getTotalAmountCollected(dropOffItems));
                dropOffInvoiceRequestDto.setStatus(dropOffItem.getStatus());
                dropOffInvoiceRequestDto.setId(dropOffInvoice.getId());
                dropOffInvoiceResponseDto = dropOffInvoiceService.updateDropOffInvoice(dropOffInvoiceRequestDto);
            }

            dropOffItemResponseDto.setDropOffInvoiceId(dropOffInvoiceResponseDto.getId());




            responseDtos.add(dropOffItemResponseDto);
        });

        return responseDtos;
    }

    public DropOffItemResponseDto updateDropOffItem(DropOffItemRequestDto request,HttpServletRequest request1) {
        validations.validateDropOffItem(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DropOffItem dropOffItem = dropOffItemRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested dropOffItem Id does not exist!"));
        InvoiceItem invoiceItem = invoiceItemRepository.getOne(request.getInvoiceItemId());
        Invoice invoice = invoiceRepository.getOne(invoiceItem.getInvoiceId());
        DropOff dropOff = dropOffRepository.getOne(dropOffItem.getDropOffId());
        mapper.map(request, dropOffItem);
        dropOffItem.setUpdatedBy(userCurrent.getId());

        if(dropOffItem.getStatus() == "completed" && dropOff.getPaymentStatus() == PaymentStatus.paid){
            invoiceItem.setVerificationStatus(VerificationStatus.verified);
        }

        if (dropOffItem.getStatus() == "pending" && dropOff.getPaymentStatus() == PaymentStatus.PayOnDelivery) {
            invoiceItem.setVerificationStatus(VerificationStatus.pending);
        }

        if (dropOffItem.getStatus() == "completed" && dropOff.getPaymentStatus() == PaymentStatus.PayOnDelivery && (dropOff.getPaymentChannel() == PaymentChannel.CASH || dropOff.getPaymentChannel() == PaymentChannel.POS)) {
            invoiceItem.setVerificationStatus(VerificationStatus.verified);
        }

        if (dropOffItem.getStatus() == "completed" && dropOff.getPaymentStatus() == PaymentStatus.PayOnDelivery && (dropOff.getPaymentChannel() == PaymentChannel.BANK_TRANSFER)) {
            invoiceItem.setVerificationStatus(VerificationStatus.AwaitingVerification);
        }

        invoiceItemRepository.save(invoiceItem);
        if(request.getQtyGoodsDelivered() != null && request.getQtyGoodsDelivered() > invoiceItem.getQty()){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Quantity of Items Delivered can't be greater than Total Quantity");
        }
        //Calculate Outstanding Amount based on Amount Collected and Total Amount
        if(request.getAmountCollected()!= null){
            if(request.getAmountCollected().doubleValue() > dropOffItem.getTotalAmount().doubleValue()){
                throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"Amount Collected can't be greater than total amount");
            }
            if (dropOffItem.getOutstandingAmount()!=null){
                dropOffItem.setOutstandingAmount(dropOffItem.getOutstandingAmount().subtract(request.getAmountCollected()));

            }
            else {
                dropOffItem.setOutstandingAmount(dropOffItem.getTotalAmount().subtract(request.getAmountCollected()));
            }

        }

        dropOffItemRepository.save(dropOffItem);
        log.debug("record updated - {}"+ new Gson().toJson(dropOffItem));
        DropOffItemResponseDto dropOffItemResponseDto = mapper.map(dropOffItem, DropOffItemResponseDto.class);

        if(request.getInvoiceItemId() != null ) {

            dropOffItemResponseDto.setInvoiceItemName(invoiceItem.getProductName());
            dropOffItemResponseDto.setQty(invoiceItem.getQty());
            //dropOffItemResponseDto.setCustomerName(invoice.getCustomerName());
            //dropOffItemResponseDto.setCustomerPhone(invoice.getCustomerPhone());
            dropOffItemResponseDto.setInvoiceId(invoiceItem.getInvoiceId());
            dropOffItemResponseDto.setDropOffId(dropOffItem.getDropOffId());
            dropOffItemResponseDto.setInvoiceItemId(dropOffItem.getInvoiceItemId());
        }

        List<DropOffItem> dropOffItems = new ArrayList<>();
        List<InvoiceItem> invoiceItems = new ArrayList<>();
        TripItem tripItem = new TripItem();
        TripItemRequestDto tripItemRequestDto = new TripItemRequestDto();
        DropOffInvoice dropOffInvoice = new DropOffInvoice();
        DropOffInvoiceRequestDto dropOffInvoiceRequestDto = new DropOffInvoiceRequestDto();
        DropOffInvoiceResponseDto dropOffInvoiceResponseDto = new DropOffInvoiceResponseDto();

        tripItem = tripItemRepository.findByTripRequestIdAndThirdPartyProductId(dropOff.getTripRequestId(), invoiceItem.getThirdPartyProductId());
        dropOffItems = dropOffItemRepository.findByTripRequestIdAndThirdPartyProductId(dropOff.getTripRequestId(), invoiceItem.getThirdPartyProductId());
        invoiceItems = invoiceItemRepository.findByInvoiceIdAndThirdPartyProductId(dropOff.getDropOffInvoice().get(0).getInvoiceId(), invoiceItem.getThirdPartyProductId());
        dropOffInvoice = dropOffInvoiceRepository.findByDropOffIdAndInvoiceId(dropOff.getId(), invoice.getId());

        if(tripItem == null) {
            tripItemRequestDto.setTripRequestId(dropOff.getTripRequestId());
            tripItemRequestDto.setThirdPartyProductId(invoiceItem.getThirdPartyProductId());
            tripItemRequestDto.setProductName(invoiceItem.getProductName());
            tripItemRequestDto.setQty(getQty(invoiceItems));
            tripItemRequestDto.setQtyPickedUp(getQtyPickedUp(dropOffItems));
            tripItemService.createTripItem(tripItemRequestDto);
        } else {
            tripItemRequestDto.setTripRequestId(dropOff.getTripRequestId());
            tripItemRequestDto.setThirdPartyProductId(invoiceItem.getThirdPartyProductId());
            tripItemRequestDto.setProductName(invoiceItem.getProductName());
            tripItemRequestDto.setQty(getQty(invoiceItems));
            tripItemRequestDto.setQtyPickedUp(getQtyPickedUp(dropOffItems));
            tripItemRequestDto.setId(tripItem.getId());
            tripItemService.updateTripItem(tripItemRequestDto);
        }


        if(dropOffInvoice == null) {
            dropOffInvoiceRequestDto.setInvoiceId(invoice.getId());
            dropOffInvoiceRequestDto.setDropOffId(dropOff.getId());
            dropOffInvoiceRequestDto.setAmount(getTotalAmount(dropOffItems));
            dropOffInvoiceRequestDto.setAmountCollected(getTotalAmountCollected(dropOffItems));
            dropOffInvoiceRequestDto.setStatus(dropOffItem.getStatus());
            dropOffInvoiceResponseDto = dropOffInvoiceService.createDropOffInvoice(dropOffInvoiceRequestDto);
        } else {
            dropOffInvoiceRequestDto.setInvoiceId(invoice.getId());
            dropOffInvoiceRequestDto.setDropOffId(dropOff.getId());
            dropOffInvoiceRequestDto.setAmount(getTotalAmount(dropOffItems));
            dropOffInvoiceRequestDto.setAmountCollected(getTotalAmountCollected(dropOffItems));
            dropOffInvoiceRequestDto.setStatus(dropOffItem.getStatus());
            dropOffInvoiceRequestDto.setId(dropOffInvoice.getId());
            dropOffInvoiceResponseDto = dropOffInvoiceService.updateDropOffInvoice(dropOffInvoiceRequestDto);
        }

        dropOffItemResponseDto.setDropOffInvoiceId(dropOffInvoiceResponseDto.getId());


        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Update dropOffItem by username:" + userCurrent.getUsername(),
                        AuditTrailFlag.UPDATE,
                        " Update dropOffItem Request for:" + dropOffItem.getId(),1, Utility.getClientIp(request1));
        return dropOffItemResponseDto;
    }

    public List<DropOffItemResponseDto> updateDropOffItemStatus(List<DropOffItemRequestDto> requests, Long dropOffId) {

        List<DropOffItemResponseDto> responseDtos = new ArrayList<>();
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        requests.forEach(request-> {
            DropOffItem dropOffItem = dropOffItemRepository.findById(request.getId())
                    .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                            "Requested dropOffItem Id does not exist!"));
            request.setDropOffId(dropOffId);
            validations.validateDropOffItem(request);
            InvoiceItem invoiceItem = invoiceItemRepository.getOne(dropOffItem.getInvoiceItemId());
            DropOff dropOff = dropOffRepository.getOne(dropOffItem.getDropOffId());

            if(request.getQtyGoodsDelivered() != null && request.getQtyGoodsDelivered() > invoiceItem.getQty()){
                throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Quantity of Items Delivered can't be greater than Total Quantity");
            }

            //Validate DropOff amountCollected for DropOffItem whose deliverytatus is either failed or PartiallyCompleted
            if (dropOff.getDeliveryStatus().equalsIgnoreCase("PartiallyCompleted") || dropOff.getDeliveryStatus().equalsIgnoreCase("completed")){
                if (request.getAmountCollected() == null)
                    throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "AmountCollected cannot be empty for PartiallyCompleted delivery");
                else if (request.getAmountCollected().longValue() <= 0)
                    throw new BadRequestException(CustomResponseCode.BAD_REQUEST,"Enusre that none of the AmountCollected for each dropoffItem is zero/negative");
                else if (request.getQtyGoodsReturned()> 0 && request.getAmountCollected().longValue() != (dropOffItem.getUnitPrice().longValue()*request.getQtyGoodsReturned()))
                    throw new BadRequestException(CustomResponseCode.BAD_REQUEST,"The amountCollected should be equal to the product of unitPrice and QtyGoodsReturned ");
                else if (request.getAmountCollected().longValue() > dropOff.getTotalAmount().longValue() || dropOffItem.getAmountCollected().longValue() < 0)
                    throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Ensure DropOffTem's AmountCollected  is neither negative and nor greaater than the DropOff's totalAmount");
            }
            //Calculate Outstanding Amount based on Amount Collected and Total Amount
            if(request.getAmountCollected()!= null){
                if(request.getAmountCollected().doubleValue() > dropOffItem.getTotalAmount().doubleValue()){
                    throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"Amount Collected can't be greater than total amount");
                }
                if (dropOffItem.getOutstandingAmount()!=null){
                    dropOffItem.setOutstandingAmount(dropOffItem.getOutstandingAmount().subtract(request.getAmountCollected()));

                }
                else {
                    dropOffItem.setOutstandingAmount(dropOffItem.getTotalAmount().subtract(request.getAmountCollected()));
                }
            }
            // Validate qtyGoodsReturned and qtyGoodsDelivered based on deliveryStatus
            Long totalQtyReturned= 0L;
            Long totalQtyDelivered = 0L;
            if (dropOff.getDeliveryStatus().equalsIgnoreCase("failed") || dropOff.getDeliveryStatus().equalsIgnoreCase("PartiallyCompleted")){
                if (dropOff.getDeliveryStatus().equalsIgnoreCase("failed")){
                        if (request.getQtyGoodsDelivered() != 0)
                            throw  new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"QtyGoodsDelivered must be 0 for failed delivery");
                        else if (request.getQtyGoodsReturned() <= 0)
                            throw  new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"QtyGoodsReturned must be gretater than  zero(0) for failed delivery");
                        if (dropOffItem.getQty() < (request.getQtyGoodsDelivered() + request.getQtyGoodsReturned()))
                            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "qty must be equal to the sum of QtyGoodsDelivered and QtyGoodsReturned");
                        dropOffItem.setQtyGoodsDelivered(request.getQtyGoodsDelivered());
                        dropOffItem.setQtyGoodsReturned(request.getQtyGoodsReturned());
                        totalQtyDelivered+=request.getQtyGoodsDelivered();
                        totalQtyReturned+=request.getQtyGoodsReturned();

                    }
                    else if (dropOff.getDeliveryStatus().equalsIgnoreCase("PartiallyCompleted")){
                        if (request.getQtyGoodsReturned() <= 0 || request.getQtyGoodsDelivered() <=0)
                            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "QtyGoodsDelivered/QtyGoodsReturned must be greater than zero(0) for PartiallyCompleted delivery");
                        if (dropOffItem.getQty() < (request.getQtyGoodsDelivered() + request.getQtyGoodsReturned()))
                            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "DropOffItem Qty must be equal to the sum of QtyGoodsDelivered and QtyGoodsReturned");
                        dropOffItem.setQtyGoodsDelivered(request.getQtyGoodsDelivered());
                        dropOffItem.setQtyGoodsReturned(request.getQtyGoodsReturned());
                        totalQtyDelivered+=request.getQtyGoodsDelivered();
                        totalQtyReturned+=request.getQtyGoodsReturned();
                    }
                    dropOff.setQtyDelivered(totalQtyDelivered.intValue());
                    dropOff.setQtyReturned(totalQtyReturned.intValue());
            }
            mapper.map(request, dropOffItem);
            dropOffItem.setUpdatedBy(userCurrent.getId());


            if(dropOffItem.getStatus() == "completed" && dropOff.getPaymentStatus() == PaymentStatus.paid){
                invoiceItem.setVerificationStatus(VerificationStatus.verified);
            }

            if (dropOffItem.getStatus() == "pending" && dropOff.getPaymentStatus() == PaymentStatus.PayOnDelivery) {
                invoiceItem.setVerificationStatus(VerificationStatus.pending);
            }

            if (dropOffItem.getStatus() == "completed" && dropOff.getPaymentStatus() == PaymentStatus.PayOnDelivery && (dropOff.getPaymentChannel() == PaymentChannel.CASH || dropOff.getPaymentChannel() == PaymentChannel.POS)) {
                invoiceItem.setVerificationStatus(VerificationStatus.verified);
            }

            if (dropOffItem.getStatus() == "completed" && dropOff.getPaymentStatus() == PaymentStatus.PayOnDelivery && (dropOff.getPaymentChannel() == PaymentChannel.BANK_TRANSFER)) {
                invoiceItem.setVerificationStatus(VerificationStatus.AwaitingVerification);
            }

            invoiceItemRepository.save(invoiceItem);


            dropOffItemRepository.save(dropOffItem);
            log.debug("record updated - {}"+ new Gson().toJson(dropOffItem));
            DropOffItemResponseDto dropOffItemResponseDto = mapper.map(dropOffItem, DropOffItemResponseDto.class);
            responseDtos.add(dropOffItemResponseDto);
        });

        return responseDtos;
    }

    public DropOffItemResponseDto findDropOffItem(Long id){
        DropOffItem dropOffItem  = dropOffItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested trip item Id does not exist!"));
        DropOffItemResponseDto dropOffItemResponseDto = mapper.map(dropOffItem, DropOffItemResponseDto.class);
        InvoiceItem invoiceItem = invoiceItemRepository.getOne(dropOffItem.getInvoiceItemId());

        Invoice invoice = invoiceRepository.getOne(invoiceItem.getInvoiceId());
        DropOff dropOff = dropOffRepository.getOne(dropOffItem.getDropOffId());
        DropOffInvoice dropOffInvoice = dropOffInvoiceRepository.findByDropOffIdAndInvoiceId(dropOff.getId(), invoice.getId());

        dropOffItemResponseDto.setInvoiceItemName(invoiceItem.getProductName());
        dropOffItemResponseDto.setQty(invoiceItem.getQty());
        //dropOffItemResponseDto.setCustomerName(invoice.getCustomerName());
        //dropOffItemResponseDto.setCustomerPhone(invoice.getCustomerPhone());
        dropOffItemResponseDto.setInvoiceId(invoiceItem.getInvoiceId());
        dropOffItemResponseDto.setDropOffId(dropOffItem.getDropOffId());
        dropOffItemResponseDto.setInvoiceItemId(dropOffItem.getInvoiceItemId());

        if(dropOffInvoice != null){
            dropOffItemResponseDto.setDropOffInvoiceId(dropOffInvoice.getId());
        }

        return dropOffItemResponseDto;
    }

    public DropOffItemResponseDto findDropOffItemDetails(Long invoiceItemId, String status){
        DropOffItem dropOffItem  = dropOffItemRepository.findByInvoiceItemIdAndStatus(invoiceItemId, status.toLowerCase());
        if(dropOffItem == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        DropOffItemResponseDto dropOffItemDetailDto = mapper.map(dropOffItem, DropOffItemResponseDto.class);
        InvoiceItem invoiceItem = invoiceItemRepository.getOne(dropOffItem.getInvoiceItemId());

        Invoice invoice = invoiceRepository.getOne(invoiceItem.getInvoiceId());

        DropOff dropOff = dropOffRepository.getOne(dropOffItem.getDropOffId());

        TripRequest tripRequest = tripRequestRepository.getOne(dropOff.getTripRequestId());

        dropOffItemDetailDto.setInvoiceItemName(invoiceItem.getProductName());
        dropOffItemDetailDto.setQty(invoiceItem.getQty());
        //dropOffItemDetailDto.setCustomerName(invoice.getCustomerName());
        //dropOffItemDetailDto.setCustomerPhone(invoice.getCustomerPhone());
        dropOffItemDetailDto.setInvoiceId(invoiceItem.getInvoiceId());
        dropOffItemDetailDto.setDropOffId(dropOffItem.getDropOffId());
        dropOffItemDetailDto.setInvoiceItemId(dropOffItem.getInvoiceItemId());
        if (tripRequest.getDriverId() != null) {
            Driver driver = driverRepository.findDriverById(tripRequest.getDriverId());

            User user = userRepository.getOne(driver.getUserId());
            dropOffItemDetailDto.setDriverName(user.getLastName() + " " + user.getFirstName());
            dropOffItemDetailDto.setDriverPhone(user.getPhone());
        }

        if (tripRequest.getDriverAssistantId() != null) {
            Driver driver2 = driverRepository.findDriverById(tripRequest.getDriverAssistantId());

            User user2 = userRepository.getOne(driver2.getUserId());
            dropOffItemDetailDto.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());
            dropOffItemDetailDto.setDriverAssistantPhone(user2.getPhone());
        }

        return dropOffItemDetailDto;
    }


    public Page<DropOffItem> findAll(Long invoiceItemId, Long dropOffId,
                                   String status, PageRequest pageRequest ){

        Page<DropOffItem> tripItems = dropOffItemRepository.findDropOffItem(invoiceItemId, dropOffId, status,  pageRequest);
        if(tripItems == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        tripItems.getContent().forEach(item ->{
            DropOffItem dropOffItem = dropOffItemRepository.getOne(item.getId());


            InvoiceItem invoiceItem = invoiceItemRepository.getOne(dropOffItem.getInvoiceItemId());

            Invoice invoice = invoiceRepository.getOne(invoiceItem.getInvoiceId());

            item.setInvoiceItemName(invoiceItem.getProductName());
            item.setQty(invoiceItem.getQty());
            //item.setCustomerName(invoice.getCustomerName());
            //item.setCustomerPhone(invoice.getCustomerPhone());
            item.setInvoiceId(invoiceItem.getInvoiceId());

        });



        return tripItems;

    }



    public void enableDisable (EnableDisEnableDto request,HttpServletRequest request1){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DropOffItem dropOffItem  = dropOffItemRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Trip item Id does not exist!"));
        dropOffItem.setIsActive(request.getIsActive());
        dropOffItem.setUpdatedBy(userCurrent.getId());

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Update dropOffItem by username:" + userCurrent.getUsername(),
                        AuditTrailFlag.UPDATE,
                        " Update dropOffItem Request for:" + dropOffItem.getId(),1, Utility.getClientIp(request1));
        dropOffItemRepository.save(dropOffItem);

    }


    public List<DropOffItem> getAll( Long dropOffId, Boolean isActive){
        List<DropOffItem> tripItems = dropOffItemRepository.findByDropOffIdAndIsActive(dropOffId, isActive);

        for (DropOffItem item : tripItems) {
            InvoiceItem invoiceItem = invoiceItemRepository.getOne(item.getInvoiceItemId());

            Invoice invoice = invoiceRepository.getOne(invoiceItem.getInvoiceId());

            item.setInvoiceItemName(invoiceItem.getProductName());
            item.setQty(invoiceItem.getQty());
            //item.setCustomerName(invoice.getCustomerName());
            //item.setCustomerPhone(invoice.getCustomerPhone());
            item.setInvoiceId(invoiceItem.getInvoiceId());

        }
        return tripItems;

    }

    public List<DropOffItem> getInvoice(Long dropOffId, Long invoiceId){

        List<DropOffItem> tripItems = dropOffItemRepository.findByDropOffIdAndInvoiceId(dropOffId, invoiceId);

        for (DropOffItem item : tripItems) {
            InvoiceItem invoiceItem = invoiceItemRepository.getOne(item.getInvoiceItemId());

            DropOff dropOff = dropOffRepository.findById(item.getDropOffId())
                    .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested tripRequestId does not exist!"));

            TripRequest tripRequest = tripRequestRepository.findById(dropOff.getTripRequestId())
                    .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested tripRequestId does not exist!"));

            Invoice invoice = invoiceRepository.getOne(invoiceItem.getInvoiceId());

            item.setInvoiceItemName(invoiceItem.getProductName());
            item.setQty(invoiceItem.getQty());
            //item.setCustomerName(invoice.getCustomerName());
            //item.setCustomerPhone(invoice.getCustomerPhone());
            item.setInvoiceId(invoiceItem.getInvoiceId());
            item.setDeliveryDate(tripRequest.getDateDelivered());
            item.setTax(invoice.getTax());
            item.setReferenceNo(tripRequest.getReferenceNo());
        }
        return tripItems;

    }

    private Integer getQty(List<InvoiceItem> invoiceItems) {
        return ((Integer)invoiceItems.stream().filter(Objects::nonNull).map(InvoiceItem::getQty).reduce(Integer.valueOf(0), Integer::sum));
    }

    private Integer getQtyPickedUp(List<DropOffItem> dropOffItems) {
        return ((Integer)dropOffItems.stream().filter(Objects::nonNull).map(DropOffItem::getQtyGoodsDelivered).reduce(Integer.valueOf(0), Integer::sum));
    }

    private BigDecimal getTotalAmount(List<DropOffItem> dropOffItems) {
        return ((BigDecimal)dropOffItems.stream().filter(Objects::nonNull).map(DropOffItem::getTotalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
    private BigDecimal getTotalAmountCollected(List<DropOffItem> dropOffItems) {
        return ((BigDecimal)dropOffItems.stream().filter(Objects::nonNull).map(DropOffItem::getAmountCollected).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
