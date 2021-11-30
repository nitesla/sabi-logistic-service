package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.repositories.RoleRepository;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.WareHouseUserRequestDto;
import com.sabi.logistics.core.dto.response.WareHouseUserResponseDto;
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

    public WarehouseUserService( UserRepository userRepository, ModelMapper mapper, Validations validations) {
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.validations = validations;
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



    public Page<WarehouseUser> findAll(Long wareHouseId, PageRequest pageRequest ){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Page<WarehouseUser> warehouseUsers = wareHouseUserRepository.findByWareHouseId(wareHouseId, pageRequest);
        if(warehouseUsers == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
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
        }
        return warehouseUsers;

    }



}
