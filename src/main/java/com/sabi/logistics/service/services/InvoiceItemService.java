package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.BadRequestException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.InvoiceItemRequestDto;
import com.sabi.logistics.core.dto.response.InvoiceItemResponseDto;
import com.sabi.logistics.core.models.Inventory;
import com.sabi.logistics.core.models.Invoice;
import com.sabi.logistics.core.models.InvoiceItem;
import com.sabi.logistics.core.models.Warehouse;
import com.sabi.logistics.service.helper.GenericSpecification;
import com.sabi.logistics.service.helper.SearchCriteria;
import com.sabi.logistics.service.helper.SearchOperation;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.InventoryRepository;
import com.sabi.logistics.service.repositories.InvoiceItemRepository;
import com.sabi.logistics.service.repositories.InvoiceRepository;
import com.sabi.logistics.service.repositories.WarehouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("All")
@Service
@Slf4j
public class InvoiceItemService {
    private final InvoiceItemRepository invoiceItemRepository;
    private final ModelMapper mapper;
    @Autowired
    private Validations validations;
    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InventoryRepository inventoryRepository;


    public InvoiceItemService(InvoiceItemRepository invoiceItemRepository, ModelMapper mapper) {
        this.invoiceItemRepository = invoiceItemRepository;
        this.mapper = mapper;
    }

    public InvoiceItemResponseDto createInvoiceItem(InvoiceItemRequestDto request) {
        validations.validateInvoiceItem(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        BigDecimal nitesla = request.getUnitPrice().multiply(new BigDecimal(request.getQty()));
        if (!request.getTotalPrice().equals(nitesla)) {
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "totalPrice MUST be equal to (unitPrice * qty)");
        }
        InvoiceItem invoiceItem = mapper.map(request,InvoiceItem.class);
        Warehouse warehouse = warehouseRepository.getOne(request.getWareHouseId());
        Invoice invoice = invoiceRepository.getOne(request.getInvoiceId());
        if (invoice!=null){
            invoiceItem.setCustomerPhone(invoice.getCustomerPhone());
            invoiceItem.setCustomerName(invoice.getCustomerName());
        }
        invoiceItem.setCreatedBy(userCurrent.getId());
        invoiceItem.setIsActive(true);
        invoiceItem.setCustomerPhone(invoice.getCustomerPhone());
        invoiceItem.setCustomerName(invoice.getCustomerName());
        invoiceItem = invoiceItemRepository.save(invoiceItem);
        log.info("Create new invoice item - {}", invoiceItem);
        InvoiceItemResponseDto invoiceItemResponseDto = mapper.map(invoiceItem, InvoiceItemResponseDto.class);
        invoiceItemResponseDto.setWareHouseName(warehouse.getName());
        return invoiceItemResponseDto;
    }

    public  List<InvoiceItemResponseDto> createInvoiceItems(List<InvoiceItemRequestDto> requests, Invoice invoice) {
        List<InvoiceItemResponseDto> responseDtos = new ArrayList<>();
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        requests.forEach(request->{
            validations.validateInvoiceItem(request);
            BigDecimal nitesla = request.getUnitPrice().multiply(new BigDecimal(request.getQty()));
            if (!request.getTotalPrice().equals(nitesla)) {
                throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "totalPrice MUST be equal to (unitPrice * qty)");
            }
            InvoiceItem invoiceItem = mapper.map(request,InvoiceItem.class);
            if (invoice!=null){
                         invoiceItem.setCustomerPhone(invoice.getCustomerPhone());
                         invoiceItem.setCustomerName(invoice.getCustomerName());
            }
            invoiceItem.setCreatedBy(userCurrent.getId());
            invoiceItem.setIsActive(true);
            invoiceItem = invoiceItemRepository.save(invoiceItem);
            responseDtos.add(mapper.map(invoiceItem, InvoiceItemResponseDto.class));
        });
        log.debug("Created new invoice items ->{}",responseDtos);
        return responseDtos;
    }

    public InvoiceItemResponseDto updateInvoiceItem(InvoiceItemRequestDto request) {
        validations.validateInvoiceItem(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        BigDecimal nitesla = request.getUnitPrice().multiply(new BigDecimal(request.getQty()));
        if (!request.getTotalPrice().equals(nitesla)) {
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "totalPrice MUST be equal to (unitPrice * qty)");
        }
        InvoiceItem invoiceItem = invoiceItemRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested invoice item Id does not exist!"));
        mapper.map(request, invoiceItem);

        invoiceItem.setUpdatedBy(userCurrent.getId());
        invoiceItemRepository.save(invoiceItem);
        log.debug("record updated - {}"+ new Gson().toJson(invoiceItem));
        InvoiceItemResponseDto invoiceItemResponseDto = mapper.map(invoiceItem, InvoiceItemResponseDto.class);
        if(request.getWareHouseId() != null ) {
            Warehouse warehouse = warehouseRepository.getOne(request.getWareHouseId());
            invoiceItemResponseDto.setWareHouseName(warehouse.getName());
        }

        return invoiceItemResponseDto;
    }

    public InvoiceItemResponseDto findInvoiceItem(Long id){
        InvoiceItem invoiceItem  = invoiceItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested invoice item Id does not exist!"));

        InvoiceItemResponseDto invoiceItemResponseDto = mapper.map(invoiceItem, InvoiceItemResponseDto.class);

        Invoice invoice = invoiceRepository.getOne(invoiceItem.getInvoiceId());
        //invoiceItemResponseDto.setCustomerName(invoice.getCustomerName());
        if (!invoice.getHasMultipleDeliveryAddress() && invoice.getHasMultipleDeliveryAddress() != null ){
            invoiceItemResponseDto.setDeliveryAddress(invoice.getDeliveryAddress());
        }
        invoiceItemResponseDto.setUnitPrice(invoiceItem.getUnitPrice());
        invoiceItemResponseDto.setTotalPrice(invoiceItem.getTotalPrice());
        invoiceItemResponseDto.setReferenceNo(invoice.getReferenceNo());

        if(invoiceItem.getInventoryId() != null) {
            Inventory inventory = inventoryRepository.getOne(invoiceItem.getInventoryId());
            invoiceItemResponseDto.setCreatedDate(inventory.getCreatedDate());
            invoiceItemResponseDto.setAcceptedDate(inventory.getAcceptedDate());
        }

        return invoiceItemResponseDto;

    }


    /**
     * @Description  includes Search by startDate, endDate and customer's Invoice name which was not initially included in the search parameters
     * @Author   Afam Okonkwo
     * @Date    11/04/2022
     * @param wareHouseId
     * @param deliveryStatus
     * @param hasInventory
     * @param productName
     * @param customerName
     * @param qty
     * @param startDate
     * @param endDate
     * @param pageRequest
     * @return InvoiceItem Page
     */
    public Page<InvoiceItem> findAll(Long wareHouseId, String deliveryStatus, Boolean hasInventory,
                                   String productName,Integer qty, LocalDateTime startDate,LocalDateTime endDate, String customerName, PageRequest pageRequest ){

        /**
        GenericSpecification<InvoiceItem> genericSpecification = new GenericSpecification<InvoiceItem>();

        if (wareHouseId != null)
        {
            genericSpecification.add(new SearchCriteria("wareHouseId", wareHouseId, SearchOperation.EQUAL));
        }


        if (deliveryStatus != null && !deliveryStatus.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("deliveryStatus", deliveryStatus, SearchOperation.EQUAL));
        }

        if (hasInventory != null) {
            genericSpecification.add(new SearchCriteria("inventoryId", 0, SearchOperation.GREATER_THAN));
        }

        if (productName != null && !productName.isEmpty())
        {
            genericSpecification.add(new SearchCriteria("productName", productName, SearchOperation.MATCH));
        }
        if (qty != null)
        genericSpecification.add(new SearchCriteria("qty", qty, SearchOperation.EQUAL));
        Page<InvoiceItem> invoiceItems = invoiceItemRepository.findAll(genericSpecification,pageRequest);
         */
        if (startDate!=null){
            if (endDate!=null && endDate.isBefore(startDate)){
                throw new BadRequestException(CustomResponseCode.BAD_REQUEST,"endDate cannot be earlier than startDate");
            }
        }
        if (endDate!=null){
            if (startDate == null){
                throw new BadRequestException(CustomResponseCode.BAD_REQUEST,"'startDate' must also be included when searching by 'endDate'");
            }
        }
        Page<InvoiceItem> invoiceItems = invoiceItemRepository.searchInvoiceItems(wareHouseId, deliveryStatus, hasInventory, productName, qty, startDate,endDate,customerName,pageRequest);
        if(invoiceItems == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        /**
         * Call the refactored re-usable method to set the needed parameters of InvoiceItems
         * @Author: Afam Okonkwo
         * @Date: 14/04/2022
         */
        invoiceItems.getContent().stream().forEach(this::setNeededParameters);
        /**
         invoiceItems.getContent().stream().forEach(item ->{
         Invoice invoice = invoiceRepository.getOne(item.getInvoiceId());
         item.setCustomerName(invoice.getCustomerName());
         item.setDeliveryAddress(invoice.getDeliveryAddress());

         if(item.getInventoryId() != null) {
         Inventory inventory = inventoryRepository.getOne(item.getInventoryId());
         item.setCreatedDate(inventory.getCreatedDate());
         item.setAcceptedDate(inventory.getAcceptedDate());
         }

         });
         */
        return invoiceItems;

    }

    public void enableDisable (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        InvoiceItem invoiceItem  = invoiceItemRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Invoice item Id does not exist!"));
        invoiceItem.setIsActive(request.isActive());
        invoiceItem.setUpdatedBy(userCurrent.getId());
        invoiceItemRepository.save(invoiceItem);

    }


    public List<InvoiceItem> getAll(Boolean isActive){
        List<InvoiceItem> invoiceItems = invoiceItemRepository.findByIsActive(isActive);
        /**
         * Call the refactored re-usable method to set the needed parameters of InvoiceItems
         * @Author: Afam Okonkwo
         * @Date: 14/04/2022
         */
        invoiceItems.stream().forEach(this::setNeededParameters);
        return invoiceItems;
        //return invoiceItemRepository.findByIsActive(isActive);
    }

    /**
     * Just normal refactoring in invoice to reuse a code section.
     * @Author Afam Okonkwo
     * @Date 14/04/2022
     * @param item
     */

    private InvoiceItem setNeededParameters(InvoiceItem item) {

            Invoice invoice = invoiceRepository.getOne(item.getInvoiceId());
            //item.setCustomerName(invoice.getCustomerName());
            if (!invoice.getHasMultipleDeliveryAddress() && invoice.getHasMultipleDeliveryAddress() != null){
            item.setDeliveryAddress(invoice.getDeliveryAddress());
            }

            if(item.getInventoryId() != null) {
                Inventory inventory = inventoryRepository.getOne(item.getInventoryId());
                item.setCreatedDate(inventory.getCreatedDate());
                item.setAcceptedDate(inventory.getAcceptedDate());
            }
            return item;
    }

    public Page<InvoiceItem> getAllDeliveries(Long partnerId, String deliveryStatus, PageRequest pageRequest ){
        GenericSpecification<InvoiceItem> genericSpecification = new GenericSpecification<InvoiceItem>();
        if (partnerId != null) {
            Warehouse warehouse = warehouseRepository.findByPartnerId(partnerId);

            if (warehouse.getId() != null) {
                genericSpecification.add(new SearchCriteria("wareHouseId", warehouse.getId(), SearchOperation.EQUAL));
            }
        }

        if (deliveryStatus != null)
        {
            genericSpecification.add(new SearchCriteria("deliveryStatus", deliveryStatus, SearchOperation.MATCH));
        }

        Page<InvoiceItem> invoiceItems = invoiceItemRepository.findAll(genericSpecification, pageRequest);
        if (invoiceItems == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No Accepted Delivery Available!");
        }

        return invoiceItems;

    }
}
