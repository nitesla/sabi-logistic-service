package com.sabi.logistics.service.services;


import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.RoleDto;
import com.sabi.framework.dto.responseDto.RoleResponseDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.helpers.CoreValidations;
import com.sabi.framework.models.Role;
import com.sabi.framework.models.User;
import com.sabi.framework.repositories.RoleRepository;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.models.PartnerRole;
import com.sabi.logistics.core.models.PartnerUser;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.PartnerRepository;
import com.sabi.logistics.service.repositories.PartnerRoleRepository;
import com.sabi.logistics.service.repositories.PartnerUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;


@SuppressWarnings("ALL")
@Slf4j
@Service
public class PartnerRoleService {


    private PartnerRepository partnerRepository;
    private RoleRepository roleRepository;
    private PartnerRoleRepository partnerRoleRepository;
    private PartnerUserRepository partnerUserRepository;
    private final CoreValidations coreValidations;
    private final ModelMapper mapper;
    private final Validations validations;

    public PartnerRoleService(PartnerRepository partnerRepository, RoleRepository roleRepository,PartnerRoleRepository partnerRoleRepository,
                              PartnerUserRepository partnerUserRepository,CoreValidations coreValidations,
                              ModelMapper mapper, Validations validations) {
        this.partnerRepository = partnerRepository;
        this.roleRepository = roleRepository;
        this.partnerRoleRepository = partnerRoleRepository;
        this.partnerUserRepository = partnerUserRepository;
        this.coreValidations = coreValidations;
        this.mapper = mapper;
        this.validations = validations;
    }


    public RoleResponseDto createPartnerRole(RoleDto request) {
        coreValidations.validateRole(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Role role = mapper.map(request,Role.class);
        PartnerUser partner = partnerUserRepository.findByUserId(userCurrent.getId());
        Role roleExist = roleRepository.findByNameAndClientId(request.getName(),partner.getPartnerId());
        if(roleExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Partner Role already exist");
        }

        role.setClientId(partner.getPartnerId());
        role.setCreatedBy(userCurrent.getId());
        role.setIsActive(true);
        role = roleRepository.save(role);
        log.debug("Create new role - {}"+ new Gson().toJson(role));

        PartnerRole partnerRole = PartnerRole.builder()
                .partnerId(role.getClientId())
                .roleId(role.getId())
                .build();
        partnerRoleRepository.save(partnerRole);

        return mapper.map(role, RoleResponseDto.class);
    }




    public Page<Role> findByClientId(String name,Boolean isActive, PageRequest pageRequest ){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();

        PartnerUser partner = partnerUserRepository.findByUserId(userCurrent.getId());
        Page<Role> roles = roleRepository.findRolesByClientId(name,partner.getPartnerId(),isActive,pageRequest);
        if(roles == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return roles;

    }



    public List<Role> getAll(Boolean isActive){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();

        PartnerUser partner = partnerUserRepository.findByUserId(userCurrent.getId());
        List<Role> roles = roleRepository.findByIsActiveAndClientId(isActive,partner.getPartnerId());
        return roles;

    }


}
