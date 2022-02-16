package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.RejectReasonDto;
import com.sabi.logistics.core.dto.response.RejectReasonResponseDto;
import com.sabi.logistics.core.models.RejectReason;
import com.sabi.logistics.service.repositories.RejectReasonRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class RejectReasonService {

    @Autowired
    private RejectReasonRepository repository;

    private final ModelMapper mapper;

    public RejectReasonService(ModelMapper mapper) {
        this.mapper = mapper;
    }

    public RejectReasonResponseDto createRejectReason(RejectReasonDto request) {

        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        RejectReason rejectReason = mapper.map(request,RejectReason.class);
        RejectReason exist = repository.findByName(request.getName());

        if(exist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " RejectReason already exist");
        }

        rejectReason.setCreatedBy(userCurrent.getId());
        rejectReason.setIsActive(true);
        rejectReason = repository.save(rejectReason);
        log.debug("Create new asset type - {}"+ new Gson().toJson(rejectReason));

        return mapper.map(rejectReason, RejectReasonResponseDto.class);
    }

    public RejectReasonResponseDto updateRejectReason(RejectReasonDto request ) {

        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        RejectReason rejectReason = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested reject reason Id does not exist!"));

        mapper.map(request, rejectReason);
        rejectReason.setUpdatedBy(userCurrent.getId());
        repository.save(rejectReason);
        log.debug("Allocations record updated - {}"+ new Gson().toJson(rejectReason));

        return mapper.map(rejectReason, RejectReasonResponseDto.class);
    }


    public RejectReasonResponseDto findRejectReason(Long id){
        RejectReason rejectReason = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested reject reason Id does not exist!"));
        return mapper.map(rejectReason, RejectReasonResponseDto.class);
    }


    public Page<RejectReason> findAll(String name, PageRequest pageRequest ){
        Page<RejectReason> rejectReasons = repository.findRejectReason(name,pageRequest);
        if(rejectReasons == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return rejectReasons;

    }


    public List<RejectReason> getAll(){
        List<RejectReason> rejectReasons = repository.findAll();
        return rejectReasons;

    }
}
