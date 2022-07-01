package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.DropOffInvoiceRequestDto;
import com.sabi.logistics.core.dto.response.DropOffInvoiceResponseDto;
import com.sabi.logistics.core.models.DropOffInvoice;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.DropOffInvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@SuppressWarnings("All")
@Slf4j
@Service
public class DropOffInvoiceService {

    private final DropOffInvoiceRepository repository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;

    public DropOffInvoiceService(DropOffInvoiceRepository repository, ModelMapper mapper, ObjectMapper objectMapper, Validations validations) {
        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
    }


    public DropOffInvoiceResponseDto createDropOffInvoice(DropOffInvoiceRequestDto request) {
        validations.validateDropOffInvoice(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DropOffInvoice dropOffInvoice = mapper.map(request,DropOffInvoice.class);
        DropOffInvoice exist = repository.findByDropOffIdAndInvoiceId(request.getDropOffId(),request.getInvoiceId());
        if(exist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "DropOff Invoice already exist");
        }

        dropOffInvoice.setCreatedBy(userCurrent.getId());
        dropOffInvoice.setIsActive(true);
        dropOffInvoice = repository.save(dropOffInvoice);
        log.debug("Create new DropOff Invoice - {}"+ new Gson().toJson(dropOffInvoice));
        return mapper.map(dropOffInvoice, DropOffInvoiceResponseDto.class);
    }


    public DropOffInvoiceResponseDto updateDropOffInvoice(DropOffInvoiceRequestDto request) {
        validations.validateDropOffInvoice(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DropOffInvoice dropOffInvoice = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested DropOff Invoice id does not exist!"));
        mapper.map(request, dropOffInvoice);

        dropOffInvoice.setUpdatedBy(userCurrent.getId());
        repository.save(dropOffInvoice);
        log.debug("DropOff Invoice record updated - {}"+ new Gson().toJson(dropOffInvoice));
        return mapper.map(dropOffInvoice, DropOffInvoiceResponseDto.class);
    }



    public DropOffInvoiceResponseDto findDropOffInvoice(Long id){
        DropOffInvoice dropOffInvoice  = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested DropOff Invoice id does not exist!"));
        return mapper.map(dropOffInvoice,DropOffInvoiceResponseDto.class);
    }

    public Page<DropOffInvoice> findAll(Long dropOffId, Long invoiceId, String status, PageRequest pageRequest ){

        Page<DropOffInvoice> dropOffInvoices = repository.findByDropOffInvoice(dropOffId, invoiceId, status, pageRequest);
        if(dropOffInvoices == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return dropOffInvoices;
    }


    public void enableDisable (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DropOffInvoice dropOffInvoice  = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested DropOff Invoice id does not exist!"));
        dropOffInvoice.setIsActive(request.getIsActive());
        dropOffInvoice.setUpdatedBy(userCurrent.getId());
        repository.save(dropOffInvoice);

    }



    public List<DropOffInvoice> getAll(Boolean isActive){
        List<DropOffInvoice> dropOffInvoices = repository.findByIsActive(isActive);
        return dropOffInvoices;

    }
}
