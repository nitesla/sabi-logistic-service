package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.sabi.logistics.core.dto.request.ClientDto;
import com.sabi.logistics.core.dto.response.ClientResponseDto;
import com.sabi.logistics.core.models.Client;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.ClientRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Service
@Slf4j
public class ClientService {


    private ClientRepository repository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;
    private final AuditTrailService auditTrailService;

    public ClientService(ClientRepository repository, ModelMapper mapper, ObjectMapper objectMapper,
                         Validations validations,AuditTrailService auditTrailService) {
        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
        this.auditTrailService = auditTrailService;
    }


    public ClientResponseDto createClient(ClientDto request,HttpServletRequest request1) {
        validations.validateClient(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Client savedClient = mapper.map(request,Client.class);
        Client exist = repository.findClientById(request.getId());
        if(exist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " client already exist");
        }
        savedClient.setCreatedBy(userCurrent.getId());
        savedClient.setIsActive(true);
        savedClient = repository.save(savedClient);
        log.debug("Create new client - {}"+ new Gson().toJson(savedClient));

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Create new client  by :" + userCurrent.getUsername(),
                        AuditTrailFlag.CREATE,
                        " Create new client for:" + savedClient.getId() ,1, Utility.getClientIp(request1));
        return mapper.map(savedClient, ClientResponseDto.class);
    }

    public ClientResponseDto updateClient(ClientDto request,HttpServletRequest request1) {
        validations.validateClient(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Client savedClient = repository.findClientById(request.getId());
        savedClient.setUpdatedBy(userCurrent.getId());
        repository.save(savedClient);
        log.debug("client record updated - {}"+ new Gson().toJson(savedClient));

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Update client by username:" + userCurrent.getUsername(),
                        AuditTrailFlag.UPDATE,
                        " Update client Request for:" + savedClient.getId(),1, Utility.getClientIp(request1));
        return mapper.map(savedClient, ClientResponseDto.class);
    }

    public ClientResponseDto findByClientId(Long id){
        Client savedClient  = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested client id does not exist!"));
        return mapper.map(savedClient,ClientResponseDto.class);
    }


    public Page<Client> findAll( Long userId, PageRequest pageRequest ){
        Page<Client> savedClient = repository.findAllClients(userId,pageRequest);
        if(savedClient == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return savedClient;
    }

    public void enableDisEnable (EnableDisEnableDto request,HttpServletRequest request1){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Client savedClient  = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested client id does not exist!"));
        savedClient.setIsActive(request.isActive());
        savedClient.setUpdatedBy(0l);

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Disable/Enable client by :" + userCurrent.getUsername() ,
                        AuditTrailFlag.UPDATE,
                        " Disable/Enable client Request for:" +  savedClient.getId()
                                ,1, Utility.getClientIp(request1));
        repository.save(savedClient);

    }


    public List<Client> getAll(Boolean isActive){
        List<Client> savedClient = repository.findByIsActive(isActive);
        return savedClient;

    }
}
