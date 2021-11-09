package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.exceptions.ConflictException;
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
import com.sabi.logistics.core.dto.request.PartnerUserRequestDto;
import com.sabi.logistics.core.dto.response.PartnerUserResponseDto;
import com.sabi.logistics.core.models.Partner;
import com.sabi.logistics.core.models.PartnerUser;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.PartnerRepository;
import com.sabi.logistics.service.repositories.PartnerUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PartnerUserService {

    @Autowired
    private PasswordEncoder passwordEncoder;
    private PartnerRepository partnerRepository;
    private UserRepository userRepository;
    private PreviousPasswordRepository previousPasswordRepository;
    private final PartnerUserRepository partnerUserRepository;
    private NotificationService notificationService;
    private final ModelMapper mapper;
    private final Validations validations;

    public PartnerUserService(PartnerRepository partnerRepository,UserRepository userRepository,PreviousPasswordRepository previousPasswordRepository,
                              PartnerUserRepository PartnerUserRepository,NotificationService notificationService,
                              ModelMapper mapper, Validations validations) {
        this.partnerRepository = partnerRepository;
        this.userRepository = userRepository;
        this.previousPasswordRepository = previousPasswordRepository;
        this.partnerUserRepository = PartnerUserRepository;
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
        String password = Utility.getSaltString();
        user.setPassword(passwordEncoder.encode(password));
        user.setUsername(request.getEmail());
        user.setCreatedBy(userCurrent.getId());
        user.setUserCategory(Constants.OTHER_USER);
        user.setIsActive(false);
        user.setLoginAttempts(0l);
        user.setResetToken(Utility.registrationCode("HHmmss"));
        user.setResetTokenExpirationDate(Utility.tokenExpiration());
        user = userRepository.save(user);
        log.debug("Create new partner user - {}"+ new Gson().toJson(user));

        PreviousPasswords previousPasswords = PreviousPasswords.builder()
                .userId(user.getId())
                .password(user.getPassword())
                .build();
        previousPasswordRepository.save(previousPasswords);

        Partner partner = partnerRepository.findByUserId(userCurrent.getId());

        PartnerUser partnerUser = PartnerUser.builder()
                .partnerId(partner.getId())
                .userId(user.getId())
                .build();
        partnerUserRepository.save(partnerUser);

        log.debug("save to partner user table - {}"+ new Gson().toJson(partnerUser));

        // --------  sending token  -----------

        NotificationRequestDto notificationRequestDto = new NotificationRequestDto();
        User emailRecipient = userRepository.getOne(user.getId());
        notificationRequestDto.setMessage("Activation Otp " + " " + user.getResetToken());
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

        return mapper.map(user, PartnerUserResponseDto.class);
    }














}
