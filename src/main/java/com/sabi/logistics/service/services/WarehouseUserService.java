package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.Role;
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



    public Page<User> findByWareHouseId(String firstName, String phone, String email, String username,
                                                   Long roleId, String lastName, PageRequest pageRequest ){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        WarehouseUser warehouse = wareHouseUserRepository.findByUserId(userCurrent.getId());
        Page<User> users = userRepository.findByWarehouseId(firstName,phone,email,username,warehouse.getWareHouseId(),lastName,pageRequest);
        if(users == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        users.getContent().forEach(wareHouseUsers ->{
            User user = userRepository.getOne(wareHouseUsers.getId());
            if(user.getRoleId() !=null){
                Role role = roleRepository.getOne(user.getRoleId());
                wareHouseUsers.setRoleName(role.getName());
            }
        });
        return users;

    }








    public List<User> getAllUsers(Boolean isActive){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();

        WarehouseUser wareHouse = wareHouseUserRepository.findByUserId(userCurrent.getId());
        List<User> users = userRepository.findByWareHouseIdAndIsActive(wareHouse.getWareHouseId(), isActive);
        for (User wareHouseUsers : users
                ) {
            if(wareHouseUsers.getRoleId() !=null){
                Role role = roleRepository.getOne(wareHouseUsers.getRoleId());
                wareHouseUsers.setRoleName(role.getName());
            }
        }
        return users;
    }



    public List<WarehouseUser> findWareHouseUsers(Long wareHouseId, Boolean isActive){
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
