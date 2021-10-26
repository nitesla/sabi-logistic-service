package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.ChangePasswordDto;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.BadRequestException;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.PreviousPasswords;
import com.sabi.framework.models.User;
import com.sabi.framework.notification.requestDto.NotificationRequestDto;
import com.sabi.framework.notification.requestDto.RecipientRequest;
import com.sabi.framework.notification.requestDto.SmsRequest;
import com.sabi.framework.repositories.PreviousPasswordRepository;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.service.NotificationService;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.Constants;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.framework.utils.Utility;
import com.sabi.logistics.core.dto.request.PartnerDto;
import com.sabi.logistics.core.dto.request.PartnerSignUpDto;
import com.sabi.logistics.core.dto.request.ResendOTP;
import com.sabi.logistics.core.dto.request.ValidateOTPRequest;
import com.sabi.logistics.core.dto.response.PartnerActivationResponse;
import com.sabi.logistics.core.dto.response.PartnerResponseDto;
import com.sabi.logistics.core.dto.response.PartnerSignUpResponseDto;
import com.sabi.logistics.core.models.Partner;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.PartnerRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Slf4j
@Service
public class PartnerService {

    @Autowired
    private PasswordEncoder passwordEncoder;
    private PartnerRepository repository;
    private UserRepository userRepository;
    private PreviousPasswordRepository previousPasswordRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;
    private NotificationService notificationService;

    public PartnerService(PartnerRepository repository,UserRepository userRepository,PreviousPasswordRepository previousPasswordRepository,
                          ModelMapper mapper, ObjectMapper objectMapper,
                          Validations validations,NotificationService notificationService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.previousPasswordRepository = previousPasswordRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
        this.notificationService = notificationService;
    }




    public PartnerSignUpResponseDto partnerSignUp(PartnerSignUpDto request) {
        validations.validatePartner(request);
        User user = mapper.map(request,User.class);

        User exist = userRepository.findByPhone(request.getPhone());
        if(exist !=null && exist.getPasswordChangedOn()== null){
            Partner existPartner = repository.findByUserId(exist.getId());
            existPartner.setRegistrationToken(Utility.registrationCode());
            existPartner.setRegistrationTokenExpiration(Utility.expiredTime());
            Partner partnerExist =repository.save(existPartner);

            NotificationRequestDto notificationRequestDto = new NotificationRequestDto();
            User emailRecipient = userRepository.getOne(exist.getId());
            notificationRequestDto.setMessage("Activation Otp " + " " + partnerExist.getRegistrationToken());
            List<RecipientRequest> recipient = new ArrayList<>();
            recipient.add(RecipientRequest.builder()
                    .email(emailRecipient.getEmail())
                    .build());
            notificationRequestDto.setRecipient(recipient);
            notificationService.emailNotificationRequest(notificationRequestDto);

            SmsRequest smsRequest = SmsRequest.builder()
                    .message("Activation Otp " + " " + partnerExist.getRegistrationToken())
                    .phoneNumber(emailRecipient.getPhone())
                    .build();
            notificationService.smsNotificationRequest(smsRequest);
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Partner user already exist, a new OTP sent to your email");

        }else if(exist !=null && exist.getPasswordChangedOn() !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Partner user already exist");
        }


        String password = Utility.getSaltString();
        user.setPassword(passwordEncoder.encode(password));
        user.setUserCategory(Constants.AGENT_USER);
        user.setUsername(request.getEmail());
        user.setLoginAttempts(0l);
        user.setCreatedBy(0l);
        user.setIsActive(false);
        user = userRepository.save(user);
        log.debug("Create new agent user - {}"+ new Gson().toJson(user));

        PreviousPasswords previousPasswords = PreviousPasswords.builder()
                .userId(user.getId())
                .password(user.getPassword())
                .build();
        previousPasswordRepository.save(previousPasswords);

        Partner savePartner = new Partner();
        savePartner.setName(request.getName());
        savePartner.setUserId(user.getId());
        savePartner.setRegistrationToken(Utility.registrationCode());
        savePartner.setRegistrationTokenExpiration(Utility.expiredTime());
        savePartner.setIsActive(false);
        savePartner.setCreatedBy(0l);

        Partner partnerResponse= repository.save(savePartner);
        log.debug("Create new partner  - {}"+ new Gson().toJson(savePartner));

// --------  sending token to agent -----------

        NotificationRequestDto notificationRequestDto = new NotificationRequestDto();
        User emailRecipient = userRepository.getOne(user.getId());
        notificationRequestDto.setMessage("Activation Otp " + " " + partnerResponse.getRegistrationToken());
        List<RecipientRequest> recipient = new ArrayList<>();
        recipient.add(RecipientRequest.builder()
                .email(emailRecipient.getEmail())
                .build());
        notificationRequestDto.setRecipient(recipient);
        notificationService.emailNotificationRequest(notificationRequestDto);

        SmsRequest smsRequest = SmsRequest.builder()
                .message("Activation Otp " + " " + partnerResponse.getRegistrationToken())
                .phoneNumber(emailRecipient.getPhone())
                .build();
        notificationService.smsNotificationRequest(smsRequest);

        return mapper.map(user, PartnerSignUpResponseDto.class);
    }




    public  void resendPartnerOTP (ResendOTP request) {
        User user = userRepository.findByPhone(request.getPhone());
        if(user == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Invalid phone number");
        }
        Partner partner = repository.findByUserId(user.getId());
        partner.setRegistrationToken(Utility.registrationCode());
        partner.setRegistrationTokenExpiration(Utility.expiredTime());
        Partner partnerResponse = repository.save(partner);

        NotificationRequestDto notificationRequestDto = new NotificationRequestDto();
        User emailRecipient = userRepository.getOne(user.getId());
        notificationRequestDto.setMessage("Activation Otp " + " " + partnerResponse.getRegistrationToken());
        List<RecipientRequest> recipient = new ArrayList<>();
        recipient.add(RecipientRequest.builder()
                .email(emailRecipient.getEmail())
                .build());
        notificationRequestDto.setRecipient(recipient);
        notificationService.emailNotificationRequest(notificationRequestDto);

        SmsRequest smsRequest = SmsRequest.builder()
                .message("Activation Otp " + " " + partnerResponse.getRegistrationToken())
                .phoneNumber(emailRecipient.getPhone())
                .build();
        notificationService.smsNotificationRequest(smsRequest);


    }






    public void validateOTP (ValidateOTPRequest request){
        Partner otpExist = repository.findByRegistrationToken(request.getRegistrationToken());
        if(otpExist ==null){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, " Invalid OTP supplied");
        }
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calobj = Calendar.getInstance();
        String currentDate = df.format(calobj.getTime());
        String regDate = otpExist.getRegistrationTokenExpiration();
        String result = String.valueOf(currentDate.compareTo(regDate));
        if(result.equals("1")){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, " OTP invalid/expired");
        }
        request.setUpdatedBy(0l);
        request.setIsActive(true);
        Partner response = partnerOTPValidation(otpExist,request);

        User userExist  = userRepository.findById(response.getUserId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested user does not exist!" + response.getUserId()));
        userExist.setIsActive(true);
        userExist.setUpdatedBy(0l);
        userRepository.save(userExist);

    }


    public Partner partnerOTPValidation(Partner partner, ValidateOTPRequest validateOTPRequest) {
        partner.setUpdatedBy(validateOTPRequest.getUpdatedBy());
        partner.setIsActive(validateOTPRequest.getIsActive());
        return repository.saveAndFlush(partner);
    }




    public PartnerActivationResponse partnerPasswordActivation(ChangePasswordDto request) {

        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested user id does not exist!"));
        mapper.map(request, user);

        String password = request.getPassword();
        user.setPassword(passwordEncoder.encode(password));
        user.setPasswordChangedOn(LocalDateTime.now());
        user = userRepository.save(user);

        PreviousPasswords previousPasswords = PreviousPasswords.builder()
                .userId(user.getId())
                .password(user.getPassword())
                .build();
        previousPasswordRepository.save(previousPasswords);

        Partner partner = repository.findByUserId(user.getId());

        PartnerActivationResponse response = PartnerActivationResponse.builder()
                .userId(user.getId())
                .partnerId(partner.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .build();

        return response;
    }









    public PartnerResponseDto createPartnerProperties(PartnerDto request) {
        validations.validatePartnerProperties(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Partner partnerProperties = mapper.map(request,Partner.class);
        Partner exist = repository.findByName(request.getName());
        if(exist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " partner properties already exist");
        }
        partnerProperties.setCreatedBy(userCurrent.getId());
        partnerProperties.setIsActive(true);
        partnerProperties = repository.save(partnerProperties);
        log.debug("Create new partner asset - {}"+ new Gson().toJson(partnerProperties));
        return mapper.map(partnerProperties, PartnerResponseDto.class);
    }


    public PartnerResponseDto updatePartnerProperties(PartnerDto request) {
        validations.validatePartnerProperties(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Partner partnerProperties = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partner properties Id does not exist!"));
        mapper.map(request, partnerProperties);
        partnerProperties.setUpdatedBy(userCurrent.getId());
        repository.save(partnerProperties);
        log.debug("partner asset record updated - {}"+ new Gson().toJson(partnerProperties));
        return mapper.map(partnerProperties, PartnerResponseDto.class);
    }


    public PartnerResponseDto findPartnerAsset(Long id){
        Partner partnerProperties  = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partner properties Id does not exist!"));
        return mapper.map(partnerProperties,PartnerResponseDto.class);
    }


    public Page<Partner> findAll(String name, PageRequest pageRequest ){
        Page<Partner> partnerProperties = repository.findPartnersProperties(name,pageRequest);
        if(partnerProperties == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return partnerProperties;

    }



    public void enableDisEnable (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Partner partnerProperties = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partner properties Id does not exist!"));
        partnerProperties.setIsActive(request.isActive());
        partnerProperties.setUpdatedBy(userCurrent.getId());
        repository.save(partnerProperties);

    }


    public List<Partner> getAll(Boolean isActive){
        List<Partner> partnerProperties = repository.findByIsActive(isActive);
        return partnerProperties;

    }
}
