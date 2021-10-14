package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.ClientDto;
import com.sabi.logistics.core.dto.response.ClientResponseDto;

import com.sabi.logistics.core.models.Client;
import com.sabi.logistics.service.repositories.ClientRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ClientService {


    private ClientRepository repository;
    private UserRepository userRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;

    public ClientService(ClientRepository repository, UserRepository userRepository, ModelMapper mapper, ObjectMapper objectMapper) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }


    public ClientResponseDto createClient(ClientDto request) {
//        validations.validateCountry(request);
        Client savedClient = mapper.map(request,Client.class);
        Client exist = repository.findClientById(request.getId());
        User savedUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                "Requested user id does not exist!"));
        if(exist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " client already exist");
        }
        if(savedUser ==null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " user record not found! ");
        }
        savedClient.setCreatedBy(0l);
        savedClient.setActive(true);
        savedClient = repository.save(savedClient);
        log.debug("Create new client - {}"+ new Gson().toJson(savedClient));
        return mapper.map(savedClient, ClientResponseDto.class);
    }

    public ClientResponseDto updateClient(ClientDto request) {
//        validations.validateCountry(request);
        Client savedClient = repository.findClientById(request.getId());
        savedClient.setUpdatedBy(0l);
        repository.save(savedClient);
        log.debug("client record updated - {}"+ new Gson().toJson(savedClient));
        return mapper.map(savedClient, ClientResponseDto.class);
    }

    public ClientResponseDto findByClientId(Long id){
        Client savedClient  = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested client id does not exist!"));
        return mapper.map(savedClient,ClientResponseDto.class);
    }


    public Page<Client> findAll(Long id, PageRequest pageRequest ){
        Page<Client> savedClient = repository.findAllClients(id,pageRequest);
        if(savedClient == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return savedClient;
    }

    public void enableDisEnable (EnableDisEnableDto request){
        Client savedClient  = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested client id does not exist!"));
        savedClient.setActive(request.isActive());
        savedClient.setUpdatedBy(0l);
        repository.save(savedClient);

    }


    public List<Client> getAll(Boolean isActive){
        List<Client> savedClient = repository.findByIsActive(isActive);
        return savedClient;

    }
}
