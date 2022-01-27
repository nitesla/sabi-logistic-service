package com.sabi.logistics.service.services;


import com.sabi.framework.dto.requestDto.GeneratePassword;
import com.sabi.framework.dto.responseDto.GeneratePasswordResponse;
import com.sabi.framework.exceptions.BadRequestException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.helpers.CoreValidations;
import com.sabi.framework.models.User;
import com.sabi.framework.notification.requestDto.NotificationRequestDto;
import com.sabi.framework.notification.requestDto.RecipientRequest;
import com.sabi.framework.notification.requestDto.SmsRequest;
import com.sabi.framework.notification.requestDto.WhatsAppRequest;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.service.NotificationService;
import com.sabi.framework.service.WhatsAppService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.framework.utils.Utility;
import com.sabi.logistics.core.models.Driver;
import com.sabi.logistics.service.repositories.DriverRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;



@SuppressWarnings("ALL")
@Slf4j
@Service
public class DriverPasswordService {


    @Value("${apple.default.password}")
    String appleDefaultPassword;

    @Value("${apple.default.phone}")
    String appleDefaultPhone;


    @Autowired
    private PasswordEncoder passwordEncoder;
    private UserRepository userRepository;
    private DriverRepository driverRepository;
    private NotificationService notificationService;
    private final ModelMapper mapper;
    private final CoreValidations coreValidations;
    private final WhatsAppService whatsAppService;



    public DriverPasswordService(UserRepository userRepository,DriverRepository driverRepository,
                       NotificationService notificationService,
                       ModelMapper mapper,CoreValidations coreValidations,
                       WhatsAppService whatsAppService) {
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.notificationService = notificationService;
        this.mapper = mapper;
        this.coreValidations = coreValidations;
        this.whatsAppService = whatsAppService;


    }






    public GeneratePasswordResponse generatePassword (GeneratePassword request) {
        coreValidations.generatePasswordValidation(request);
        User userPhone = userRepository.findByPhone(request.getPhone());
        if (userPhone == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Phone number does not exist");
        }
        if (userPhone.getIsActive() == false) {
            throw new BadRequestException(CustomResponseCode.FAILED, "User account has been disabled");
        }

        Driver driver = driverRepository.findByUserId(userPhone.getId());
        if(driver == null){
            throw new BadRequestException(CustomResponseCode.FAILED, "User is not a driver");
        }
        if(userPhone.getPhone().equals(appleDefaultPhone)){
            GeneratePasswordResponse generatePasswordResponse = GeneratePasswordResponse.builder()
                    .username(userPhone.getUsername())
                    .defaultApplePassword(appleDefaultPassword)
                    .build();
            return generatePasswordResponse;
        }

        String generatePassword= Utility.passwordGeneration();
        userPhone.setPassword(passwordEncoder.encode(generatePassword));
        userPhone.setPasswordExpiration(Utility.passwordExpiration());
        userRepository.save(userPhone);

        NotificationRequestDto notificationRequestDto = new NotificationRequestDto();
        User emailRecipient = userRepository.getOne(userPhone.getId());
        notificationRequestDto.setMessage("One time password " + " " + generatePassword);
        List<RecipientRequest> recipient = new ArrayList<>();
        recipient.add(RecipientRequest.builder()
                .email(emailRecipient.getEmail())
                .build());
        notificationRequestDto.setRecipient(recipient);
        notificationService.emailNotificationRequest(notificationRequestDto);

        SmsRequest smsRequest = SmsRequest.builder()
                .message("One time password " + " " + generatePassword)
                .phoneNumber(emailRecipient.getPhone())
                .build();
        notificationService.smsNotificationRequest(smsRequest);

        WhatsAppRequest whatsAppRequest = WhatsAppRequest.builder()
                .message("One time password " + " " + generatePassword)
                .phoneNumber(emailRecipient.getPhone())
                .build();
        whatsAppService.whatsAppNotification(whatsAppRequest);

        GeneratePasswordResponse response = GeneratePasswordResponse.builder()
                .username(userPhone.getUsername())
                .build();
        return response;
    }




    public void validateGeneratedPassword (Long id) {
        User userExist = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " user id does not exist!"));
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calobj = Calendar.getInstance();
        String currentDate = df.format(calobj.getTime());
        String passDate = userExist.getPasswordExpiration();
        String result = String.valueOf(currentDate.compareTo(passDate));
        if(result.equals("1")){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, " Password has expired");
        }
    }
}
