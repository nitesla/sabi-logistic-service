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
import com.sabi.logistics.core.dto.request.ColorRequestDto;
import com.sabi.logistics.core.dto.response.ColorResponseDto;
import com.sabi.logistics.core.models.Color;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.ColorRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Service
@Slf4j
public class ColorService {
    private final ColorRepository colorRepository;
    private final ModelMapper mapper;
    private final Validations validations;
    private final AuditTrailService auditTrailService;



    public ColorService(ColorRepository colorRepository, ModelMapper mapper, Validations validations,
                        AuditTrailService auditTrailService) {
        this.colorRepository = colorRepository;
        this.mapper = mapper;
        this.validations = validations;
        this.auditTrailService = auditTrailService;
    }

    public ColorResponseDto createColor(ColorRequestDto request,HttpServletRequest request1) {
        validations.validateColor(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Color color = mapper.map(request,Color.class);
        Color colorExists = colorRepository.findByName(request.getName());
        if(colorExists !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " color already exist");
        }
        color.setCreatedBy(userCurrent.getId());
        color.setIsActive(true);
        color = colorRepository.save(color);
        log.debug("Create new color - {}"+ new Gson().toJson(color));

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Create new color  by :" + userCurrent.getUsername(),
                        AuditTrailFlag.CREATE,
                        " Create new color for:" + color.getName() ,1, Utility.getClientIp(request1));
        return mapper.map(color, ColorResponseDto.class);
    }

    public ColorResponseDto updateColor(ColorRequestDto request,HttpServletRequest request1) {
        validations.validateColor(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Color color = colorRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested color Id does not exist!"));
        mapper.map(request, color);
        color.setUpdatedBy(userCurrent.getId());
        colorRepository.save(color);
        log.debug("color record updated - {}"+ new Gson().toJson(color));

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Update color by username:" + userCurrent.getUsername(),
                        AuditTrailFlag.UPDATE,
                        " Update color Request for:" + color.getId(),1, Utility.getClientIp(request1));   auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Update color by username:" + userCurrent.getUsername(),
                        AuditTrailFlag.UPDATE,
                        " Update color Request for:" + color.getId(),1, Utility.getClientIp(request1));
        return mapper.map(color, ColorResponseDto.class);
    }

    public ColorResponseDto findColor(Long id){
        Color color  = colorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested color Id does not exist!"));
        return mapper.map(color, ColorResponseDto.class);
    }


    public Page<Color> findAll(String name, PageRequest pageRequest ){
        Page<Color> colors = colorRepository.findColor(name,pageRequest);
        if(colors == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return colors;

    }



    public void enableDisEnableState (EnableDisEnableDto request,HttpServletRequest request1){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Color Color  = colorRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Color Id does not exist!"));
        Color.setIsActive(request.isActive());
        Color.setUpdatedBy(userCurrent.getId());

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Disable/Enable Color by :" + userCurrent.getUsername() ,
                        AuditTrailFlag.UPDATE,
                        " Disable/Enable Color Request for:" +  Color.getId()
                                + " " +  Color.getName(),1, Utility.getClientIp(request1));
        colorRepository.save(Color);

    }


    public List<Color> getAll(Boolean isActive){
        List<Color> Colors = colorRepository.findByIsActive(isActive);
        return Colors;

    }
}
