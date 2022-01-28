package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.Role;
import com.sabi.framework.models.User;
import com.sabi.framework.repositories.RoleRepository;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.service.AuditTrailService;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.WareHouseUserRequestDto;
import com.sabi.logistics.core.dto.response.WareHouseUserResponseDto;
import com.sabi.logistics.core.models.Warehouse;
import com.sabi.logistics.core.models.WarehouseUser;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.WarehouseRepository;
import com.sabi.logistics.service.repositories.WarehouseUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ALL")
@Slf4j
@Service
public class WarehouseUserService {
    
    private UserRepository userRepository;
    private final ModelMapper mapper;
    private final Validations validations;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private WarehouseRepository wareHouseRepository;
    @Autowired
    private WarehouseUserRepository wareHouseUserRepository;
    @Autowired
    private RoleRepository roleRepository;
    private final AuditTrailService auditTrailService;

    public WarehouseUserService( UserRepository userRepository, ModelMapper mapper, Validations validations,AuditTrailService auditTrailService) {
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.validations = validations;
        this.auditTrailService = auditTrailService;
    }

    public List<WareHouseUserResponseDto> createWareHouseUser(List<WareHouseUserRequestDto> requests) {

        List<WareHouseUserResponseDto> warehouseUsers = new ArrayList<>();
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        requests.forEach(request->{
            WarehouseUser warehouseUser =  mapper.map(request, WarehouseUser.class);
            warehouseUser.setUpdatedBy(userCurrent.getId());
            warehouseUser.setIsActive(true);
            wareHouseUserRepository.save(warehouseUser);
            log.debug("warehouseUser record updated - {}" + new Gson().toJson(warehouseUser));
            warehouseUsers.add(mapper.map(warehouseUser, WareHouseUserResponseDto.class));

        });

        return warehouseUsers;
    }

    public WareHouseUserResponseDto updateWareHouseUser(WareHouseUserRequestDto request) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        WarehouseUser warehouseUser = wareHouseUserRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested warehouseUser Id does not exist!"));
        mapper.map(request, warehouseUser);
        warehouseUser.setUpdatedBy(userCurrent.getId());
        wareHouseUserRepository.save(warehouseUser);
        log.debug("warehouseUser record updated - {}" + new Gson().toJson(warehouseUser));
        return mapper.map(warehouseUser, WareHouseUserResponseDto.class);
    }

    public WareHouseUserResponseDto deleteWareHouseUser(Long id){
        WarehouseUser wareHouseUser = wareHouseUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested WareHouseUser Id does not exist!"));
        wareHouseUserRepository.deleteById(wareHouseUser.getId());
        log.debug("WareHouse User Deleted - {}"+ new Gson().toJson(wareHouseUser));
        return mapper.map(wareHouseUser, WareHouseUserResponseDto.class);
    }

    public Page<WarehouseUser> findAll(Long wareHouseId, PageRequest pageRequest ){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Page<WarehouseUser> warehouseUsers = wareHouseUserRepository.findByWareHouseId(wareHouseId, pageRequest);
        if(warehouseUsers == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }

        warehouseUsers.getContent().forEach(users -> {
            User user = userRepository.getOne(users.getUserId());
            if(user.getRoleId() !=null){
                Role role = roleRepository.getOne(user.getRoleId());
                users.setRoleName(role.getName());
            }

            Warehouse warehouse = wareHouseRepository.findWarehouseById(wareHouseId);
            users.setWareHouseName(warehouse.getName());
            users.setEmail(user.getEmail());
            users.setFirstName(user.getFirstName());
            users.setLastName(user.getLastName());
            users.setPhone(user.getPhone());
            users.setMiddleName(user.getMiddleName());
            users.setUsername(user.getUsername());
            users.setRoleId(user.getRoleId());
        });

        return warehouseUsers;

    }



    public List<WarehouseUser> getAll(Long wareHouseId, Boolean isActive){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();

        List<WarehouseUser> warehouseUsers = wareHouseUserRepository.findByWareHouseIdAndIsActive(wareHouseId, isActive);
        for (WarehouseUser users : warehouseUsers
                ) {
            User user = userRepository.getOne(users.getUserId());
            users.setEmail(user.getEmail());
            users.setFirstName(user.getFirstName());
            users.setLastName(user.getLastName());
            users.setPhone(user.getPhone());
            users.setMiddleName(user.getMiddleName());
            users.setUsername(user.getUsername());
            if(user.getRoleId() !=null){
                Role role = roleRepository.getOne(user.getRoleId());
                users.setRoleName(role.getName());
            }
            users.setRoleId(user.getRoleId());
            Warehouse warehouse = wareHouseRepository.findWarehouseById(wareHouseId);
            users.setWareHouseName(warehouse.getName());
        }
        return warehouseUsers;

    }



}
