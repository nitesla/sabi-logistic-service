package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.PreviousPasswords;
import com.sabi.framework.models.Role;
import com.sabi.framework.models.User;
import com.sabi.framework.models.UserRole;
import com.sabi.framework.notification.requestDto.NotificationRequestDto;
import com.sabi.framework.notification.requestDto.RecipientRequest;
import com.sabi.framework.notification.requestDto.SmsRequest;
import com.sabi.framework.notification.requestDto.WhatsAppRequest;
import com.sabi.framework.repositories.PreviousPasswordRepository;
import com.sabi.framework.repositories.RoleRepository;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.repositories.UserRoleRepository;
import com.sabi.framework.service.AuditTrailService;
import com.sabi.framework.service.NotificationService;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.service.WhatsAppService;
import com.sabi.framework.utils.AuditTrailFlag;
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

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
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
    private final AuditTrailService auditTrailService;
    private final WhatsAppService whatsAppService;
    private final UserRoleRepository userRoleRepository;

    public PartnerUserService(PartnerRepository partnerRepository,UserRepository userRepository,PreviousPasswordRepository previousPasswordRepository,
                              PartnerUserRepository partnerUserRepository,DriverRepository driverRepository,
                              RoleRepository roleRepository,
                              NotificationService notificationService,
                              ModelMapper mapper, Validations validations,AuditTrailService auditTrailService,
                              WhatsAppService whatsAppService,UserRoleRepository userRoleRepository) {
        this.partnerRepository = partnerRepository;
        this.userRepository = userRepository;
        this.previousPasswordRepository = previousPasswordRepository;
        this.partnerUserRepository = partnerUserRepository;
        this.driverRepository = driverRepository;
        this.roleRepository = roleRepository;
        this.notificationService = notificationService;
        this.mapper = mapper;
        this.validations = validations;
        this.auditTrailService = auditTrailService;
        this.whatsAppService = whatsAppService;
        this.userRoleRepository = userRoleRepository;

    }

    public PartnerUserResponseDto createPartnerUser(PartnerUserRequestDto request,HttpServletRequest request1) {
        validations.validatePartnerUser(request);
        User user = mapper.map(request,User.class);

        User userExist = userRepository.findByEmailOrPhone(request.getEmail(),request.getPhone());
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
        user.setLoginAttempts(0);
        user = userRepository.save(user);
        log.debug("Create new partner user - {}"+ new Gson().toJson(user));

        UserRole userRole = UserRole.builder()
                .userId(user.getId())
                .roleId(user.getRoleId())
                .createdDate(LocalDateTime.now())
                .build();
        userRoleRepository.save(userRole);

        PreviousPasswords previousPasswords = PreviousPasswords.builder()
                .userId(user.getId())
                .password(user.getPassword())
                .createdDate(LocalDateTime.now())
                .build();
        previousPasswordRepository.save(previousPasswords);

        PartnerUser partnerUser = new PartnerUser();
        partnerUser.setPartnerId(partner.getPartnerId());
        partnerUser.setUserId(user.getId());
        partnerUser.setCreatedBy(userCurrent.getId());
        partnerUser.setUserType(request.getUserType());
        partnerUser.setIsActive(false);
        partnerUserRepository.save(partnerUser);
        log.debug("save to partner user table - {}"+ new Gson().toJson(partnerUser));

        if(request.getUserType().equalsIgnoreCase(PartnerConstants.DRIVER_USER)){
            Driver driver = new Driver();
            driver.setPartnerId(partner.getPartnerId());
            driver.setUserId(user.getId());
            driver.setIsActive(true);
            driver.setCreatedBy(userCurrent.getId());
           Driver driver1= driverRepository.save(driver);
         User user1 = userRepository.getOne(driver.getUserId());
              user1.setIsActive(true);
              user1.setPasswordChangedOn(LocalDateTime.now());
              userRepository.save(user1);
        }


        PartnerUserResponseDto partnerUserResponseDto = PartnerUserResponseDto.builder()
                .createdBy(user.getCreatedBy())
                .createdDate(user.getCreatedDate())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .isActive(user.getIsActive())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .updatedBy(user.getUpdatedBy())
                .updatedDate(user.getUpdatedDate())
                .userType(request.getUserType())
                .build();


        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Create new partner user by :" + userCurrent.getUsername(),
                        AuditTrailFlag.CREATE,
                        " Create new partner user for:" + user.getFirstName() + " " + user.getUsername(),1, Utility.getClientIp(request1));


        return partnerUserResponseDto;
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
                    .message(msg)
                    .phoneNumber(emailRecipient.getPhone())
                    .build();
            notificationService.smsNotificationRequest(smsRequest);

             WhatsAppRequest whatsAppRequest = WhatsAppRequest.builder()
                     .message(msg)
                     .phoneNumber(emailRecipient.getPhone())
                     .build();
             whatsAppService.whatsAppNotification(whatsAppRequest);



    }





    public Page<User> findByClientId(String firstName, String phone, String email, String username,
                                                   Long roleId,Boolean isActive, String lastName, PageRequest pageRequest ){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerUser partner = partnerUserRepository.findByUserId(userCurrent.getId());
        Page<User> users = userRepository.findByClientId(firstName,phone,email,username,roleId,partner.getPartnerId(),isActive,lastName,pageRequest);
        if(users == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        users.getContent().forEach(partnerUsers ->{
            User user = userRepository.getOne(partnerUsers.getId());
            PartnerUser partnerUser = partnerUserRepository.findByUserId(partnerUsers.getId());
            if(user.getRoleId() !=null){
                Role role = roleRepository.getOne(user.getRoleId());
                partnerUsers.setRoleName(role.getName());
            }
            partnerUsers.setUserType(partnerUser.getUserType());
        });
        return users;

    }


    public Page<PartnerUser> findPartnerUsers(String userType,Boolean isActive, PageRequest pageRequest ){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();

        PartnerUser partner = partnerUserRepository.findByUserId(userCurrent.getId());
        Page<PartnerUser> partnerUsers = partnerUserRepository.findPartnerUsers(partner.getPartnerId(),userType,isActive,pageRequest);
        if(partnerUsers == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        partnerUsers.getContent().forEach(users -> {
         User user = userRepository.getOne(users.getUserId());
            if(user.getRoleId() !=null){
                Role role = roleRepository.getOne(user.getRoleId());
             users.setRoleName(role.getName());
         }
         users.setUserType(users.getUserType());
         users.setEmail(user.getEmail());
         users.setFirstName(user.getFirstName());
         users.setLastName(user.getLastName());
         users.setPhone(user.getPhone());
         users.setMiddleName(user.getMiddleName());
         users.setUsername(user.getUsername());
         users.setRoleId(user.getRoleId());
         users.setLoginAttempts(Long.valueOf(user.getLoginAttempts()));
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
            PartnerUser partnerUserType = partnerUserRepository.findByUserId(partnerUsers.getId());
            partnerUsers.setUserType(partnerUserType.getUserType());
        }
        return users;
    }



    public List<PartnerUser> findPartnerUsersList(String userType,Boolean isActive){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();

        PartnerUser partner = partnerUserRepository.findByUserId(userCurrent.getId());
        List<PartnerUser> partnerUsers = partnerUserRepository.findPartnerUsersList(partner.getPartnerId(),userType,isActive);
        for (PartnerUser users : partnerUsers
                ) {
            User user = userRepository.getOne(users.getUserId());
            if(user.getRoleId() !=null){
                Role role = roleRepository.getOne(user.getRoleId());
                users.setRoleName(role.getName());
            }

            users.setUserType(users.getUserType());
            users.setEmail(user.getEmail());
            users.setFirstName(user.getFirstName());
            users.setLastName(user.getLastName());
            users.setPhone(user.getPhone());
            users.setMiddleName(user.getMiddleName());
            users.setUsername(user.getUsername());
            users.setRoleId(user.getRoleId());
            users.setLoginAttempts(Long.valueOf(user.getLoginAttempts()));
            users.setFailedLoginDate(user.getFailedLoginDate());
            users.setLastLogin(user.getLastLogin());
            users.setLockedDate(user.getLockedDate());
        }
        return partnerUsers;

    }



}
