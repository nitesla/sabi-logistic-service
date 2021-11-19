package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.PreviousPasswords;
import com.sabi.framework.models.Role;
import com.sabi.framework.models.User;
import com.sabi.framework.notification.requestDto.NotificationRequestDto;
import com.sabi.framework.notification.requestDto.RecipientRequest;
import com.sabi.framework.notification.requestDto.SmsRequest;
import com.sabi.framework.repositories.PreviousPasswordRepository;
import com.sabi.framework.repositories.RoleRepository;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.service.NotificationService;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.Constants;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.framework.utils.Utility;
import com.sabi.logistics.core.dto.request.PartnerUserActivation;
import com.sabi.logistics.core.dto.request.PartnerUserRequestDto;
import com.sabi.logistics.core.dto.response.PartnerUserResponseDto;
import com.sabi.logistics.core.models.Driver;
import com.sabi.logistics.core.models.PartnerUser;
import com.sabi.logistics.service.helper.PartnerConstants;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.DriverRepository;
import com.sabi.logistics.service.repositories.PartnerRepository;
import com.sabi.logistics.service.repositories.PartnerUserRepository;
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
public class PartnerUserService {

    @Autowired
    private PasswordEncoder passwordEncoder;
    private PartnerRepository partnerRepository;
    private UserRepository userRepository;
    private PreviousPasswordRepository previousPasswordRepository;
    private final PartnerUserRepository partnerUserRepository;
    private DriverRepository driverRepository;
    private RoleRepository roleRepository;
    private NotificationService notificationService;
    private final ModelMapper mapper;
    private final Validations validations;

    public PartnerUserService(PartnerRepository partnerRepository,UserRepository userRepository,PreviousPasswordRepository previousPasswordRepository,
                              PartnerUserRepository partnerUserRepository,DriverRepository driverRepository,
                              RoleRepository roleRepository,
                              NotificationService notificationService,
                              ModelMapper mapper, Validations validations) {
        this.partnerRepository = partnerRepository;
        this.userRepository = userRepository;
        this.previousPasswordRepository = previousPasswordRepository;
        this.partnerUserRepository = partnerUserRepository;
        this.driverRepository = driverRepository;
        this.roleRepository = roleRepository;
        this.notificationService = notificationService;
        this.mapper = mapper;
        this.validations = validations;
    }

    public PartnerUserResponseDto createPartnerUser(PartnerUserRequestDto request) {
        validations.validatePartnerUser(request);
        User user = mapper.map(request,User.class);

        User userExist = userRepository.findByFirstNameAndLastName(request.getFirstName(), request.getLastName());
        if(userExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " User already exist");
        }
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();

        PartnerUser partner = partnerUserRepository.findByUserId(userCurrent.getId());

        String password = Utility.getSaltString();
        user.setPassword(passwordEncoder.encode(password));
        user.setUsername(request.getEmail());
        user.setCreatedBy(userCurrent.getId());
        user.setUserCategory(Constants.OTHER_USER);
        user.setClientId(partner.getPartnerId());
        user.setIsActive(false);
        user.setLoginAttempts(0l);
//        user.setResetToken(Utility.registrationCode("HHmmss"));
//        user.setResetTokenExpirationDate(Utility.tokenExpiration());
        user = userRepository.save(user);
        log.debug("Create new partner user - {}"+ new Gson().toJson(user));

        PreviousPasswords previousPasswords = PreviousPasswords.builder()
                .userId(user.getId())
                .password(user.getPassword())
                .build();
        previousPasswordRepository.save(previousPasswords);

        PartnerUser partnerUser = new PartnerUser();
        partnerUser.setPartnerId(user.getClientId());
        partnerUser.setUserId(user.getId());
        partnerUser.setCreatedBy(userCurrent.getId());
        partnerUser.setUserType(request.getUserType());
        partnerUser.setIsActive(true);
        partnerUserRepository.save(partnerUser);
        log.debug("save to partner user table - {}"+ new Gson().toJson(partnerUser));

        if(request.getUserType().equalsIgnoreCase(PartnerConstants.DRIVER_USER)){
            Driver driver = Driver.builder()
                    .partnerId(user.getClientId())
                    .userId(user.getId())
                    .build();
            driverRepository.save(driver);
        }
        return mapper.map(user, PartnerUserResponseDto.class);
    }





         public  void activatePartnerUser (PartnerUserActivation request) {
              validations.validatePartnerUserActivation(request);
            User user = userRepository.findByEmail(request.getEmail());
            if (user == null) {
                throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Invalid email");
            }
            user.setResetToken(Utility.registrationCode("HHmmss"));
            user.setResetTokenExpirationDate(Utility.tokenExpiration());
            userRepository.save(user);

             String msg = "Hello " + " " + user.getFirstName() + " " + user.getLastName() + "<br/>"
                     + "Username :" + " "+ user.getUsername() + "<br/>"
                     + "Activation OTP :" + " "+ user.getResetToken() + "<br/>"
                     + " Kindly click the link below to complete your registration " + "<br/>"
                     + "<a href=\"" + request.getActivationUrl() +  "\">Activate your account</a>";

            NotificationRequestDto notificationRequestDto = new NotificationRequestDto();
            User emailRecipient = userRepository.getOne(user.getId());
            notificationRequestDto.setMessage(msg);
            List<RecipientRequest> recipient = new ArrayList<>();
            recipient.add(RecipientRequest.builder()
                    .email(emailRecipient.getEmail())
                    .build());
            notificationRequestDto.setRecipient(recipient);
            notificationService.emailNotificationRequest(notificationRequestDto);


            SmsRequest smsRequest = SmsRequest.builder()
                    .message("Activation Otp " + " " + user.getResetToken())
                    .phoneNumber(emailRecipient.getPhone())
                    .build();
            notificationService.smsNotificationRequest(smsRequest);



    }





    public Page<User> findByClientId(String firstName, String phone, String email, String username,
                                                   Long roleId, String lastName, PageRequest pageRequest ){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();

        PartnerUser partner = partnerUserRepository.findByUserId(userCurrent.getId());
        Page<User> users = userRepository.findByClientId(firstName,phone,email,username,roleId,partner.getPartnerId(),lastName,pageRequest);
        if(users == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        users.getContent().forEach(partnerUsers ->{
            User user = userRepository.getOne(partnerUsers.getId());
            if(user.getRoleId() !=null){
                Role role = roleRepository.getOne(user.getRoleId());
                partnerUsers.setRoleName(role.getName());
            }
        });
        return users;

    }


    public Page<PartnerUser> findPartnerUsers(String userType, PageRequest pageRequest ){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();

        PartnerUser partner = partnerUserRepository.findByUserId(userCurrent.getId());
        Page<PartnerUser> partnerUsers = partnerUserRepository.findPartnerUsers(partner.getPartnerId(),userType,pageRequest);
        if(partnerUsers == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        partnerUsers.getContent().forEach(users -> {

         User user = userRepository.getOne(users.getUserId());
            if(user.getRoleId() !=null){
                Role role = roleRepository.getOne(user.getRoleId());
             users.setRoleName(role.getName());
         }
         users.setEmail(user.getEmail());
         users.setFirstName(user.getFirstName());
         users.setLastName(user.getLastName());
         users.setPhone(user.getPhone());
         users.setMiddleName(user.getMiddleName());
         users.setUsername(user.getUsername());
         users.setRoleId(user.getRoleId());
         users.setLoginAttempts(user.getLoginAttempts());
         users.setFailedLoginDate(user.getFailedLoginDate());
         users.setLastLogin(user.getLastLogin());
         users.setLockedDate(user.getLockedDate());

        });

            return partnerUsers;

    }





    public List<User> getAll(Boolean isActive){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();

        PartnerUser partner = partnerUserRepository.findByUserId(userCurrent.getId());
        List<User> users = userRepository.findByIsActiveAndClientId(isActive,partner.getPartnerId());
        for (User partnerUsers : users
                ) {

            if(partnerUsers.getRoleId() !=null){
                Role role = roleRepository.getOne(partnerUsers.getRoleId());
                partnerUsers.setRoleName(role.getName());
            }
        }

        return users;

    }



    public List<PartnerUser> findPartnerUsersList(String userType){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();

        PartnerUser partner = partnerUserRepository.findByUserId(userCurrent.getId());
        List<PartnerUser> partnerUsers = partnerUserRepository.findPartnerUsersList(partner.getPartnerId(),userType);

        for (PartnerUser users : partnerUsers
                ) {
            User user = userRepository.getOne(users.getUserId());

            if(user.getRoleId() !=null){
                Role role = roleRepository.getOne(user.getRoleId());
                users.setRoleName(role.getName());
            }

            users.setEmail(user.getEmail());
            users.setFirstName(user.getFirstName());
            users.setLastName(user.getLastName());
            users.setPhone(user.getPhone());
            users.setMiddleName(user.getMiddleName());
            users.setUsername(user.getUsername());
            users.setRoleId(user.getRoleId());

            users.setLoginAttempts(user.getLoginAttempts());
            users.setFailedLoginDate(user.getFailedLoginDate());
            users.setLastLogin(user.getLastLogin());
            users.setLockedDate(user.getLockedDate());
        }
        return partnerUsers;

    }



}
