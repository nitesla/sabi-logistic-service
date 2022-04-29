package com.sabi.logistics.service.services;

import com.sabi.framework.helpers.API;
import com.sabi.framework.models.PreviousPasswords;
import com.sabi.framework.models.User;
import com.sabi.framework.notification.requestDto.NotificationRequestDto;
import com.sabi.framework.notification.requestDto.RecipientRequest;
import com.sabi.framework.notification.requestDto.SmsRequest;
import com.sabi.framework.notification.requestDto.WhatsAppRequest;
import com.sabi.framework.repositories.PreviousPasswordRepository;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.service.NotificationService;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.service.WhatsAppService;
import com.sabi.framework.utils.Constants;
import com.sabi.framework.utils.Utility;
import com.sabi.logistics.core.dto.request.AdminAuthDto;
import com.sabi.logistics.core.dto.response.admin.AccessTokenWithUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;

@Service
@Slf4j
public class AdminUserServiceImp implements AdminUserService {

    private final UserRepository userRepository;

    private final PreviousPasswordRepository previousPasswordRepository;

    private final API api;

    private final NotificationService notificationService;

    private final WhatsAppService whatsAppService;

    private final PasswordEncoder passwordEncoder;

    private final TokenService tokenService;
    @Value("${globalAdmin.auth.url}")
    private String globalAdminAuthUrl;


    public AdminUserServiceImp(UserRepository userRepository, PreviousPasswordRepository previousPasswordRepository, API api, NotificationService notificationService, WhatsAppService whatsAppService, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.previousPasswordRepository = previousPasswordRepository;
        this.api = api;
        this.notificationService = notificationService;
        this.whatsAppService = whatsAppService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }


    @Override
    public AccessTokenWithUserDetails createOrLoginAdmin(AdminAuthDto adminAuthDto) {

        // call the global token api first to authenticate the user
        AccessTokenWithUserDetails accessTokenWithUserDetails = this.api.post(globalAdminAuthUrl,adminAuthDto,AccessTokenWithUserDetails.class,null);

        //Is the user authenticated on globalAdmin
        if (accessTokenWithUserDetails != null){
            User user = userRepository.findByEmail(accessTokenWithUserDetails.getEmail());

            if (user != null){
                //The user already exists. Just Login
            }
            //The user doesn't exist yet, register him/her.
            else {
                user.setFirstName(accessTokenWithUserDetails.getFirstName());
                user.setLastName(accessTokenWithUserDetails.getLastName());
                user.setUsername(accessTokenWithUserDetails.getEmail());
                user.setUserCategory(Constants.ADMIN_USER);
                user.setPhone(accessTokenWithUserDetails.getPhone());
                String defaultPassword = Utility.passwordGeneration();
                user.setPassword(passwordEncoder.encode(defaultPassword));
                user.setGlobalAdminUserId(accessTokenWithUserDetails.getGlobalAdminUserId());
                user.setLoginAttempts(0);
                user.setIsActive(true);
                user.setPasswordChangedOn(LocalDateTime.now());
                user.setCreatedBy(0L);
                user.setCreatedDate(LocalDateTime.now());
                user.setUpdatedDate(LocalDateTime.now());
                user.setResetToken(Utility.registrationCode("HHmmss"));
                user.setResetTokenExpirationDate(Utility.tokenExpiration());
                user = userRepository.save(user);

                PreviousPasswords previousPasswords = PreviousPasswords.builder().userId(user.getId())
                        .password(defaultPassword).createdDate(LocalDateTime.now()).build();
                previousPasswordRepository.save(previousPasswords);

                this.dispatchNotificationsToAdmin(user,"Sabi:\nYour Admin Account has been created. Your default password is:"+defaultPassword+" \n\nLogin to change your default password");
                accessTokenWithUserDetails.setUserId(user.getId());
                accessTokenWithUserDetails.setGlobalAdminToken(accessTokenWithUserDetails.getAccessToken());
                String newToken = "Bearer "+this.tokenService.generateNewToken(); //application specific token
                accessTokenWithUserDetails.setAccessToken(newToken);// logistics generated appToken.

            }
        }
        return accessTokenWithUserDetails;
    }

    @Override
    public void dispatchNotificationsToAdmin(User user, String message) {

        dispatchNotification(user, message, notificationService,whatsAppService);

        log.info("Notification successfully sent to Admin {} ",user.getFirstName());

    }

    static void dispatchNotification(User user, String message, NotificationService notificationService, WhatsAppService whatsAppService) {
        NotificationRequestDto notificationRequestDto = new NotificationRequestDto();
        notificationRequestDto.setEmail(true);
        RecipientRequest recipientRequest = new RecipientRequest();
        recipientRequest.setEmail(user.getEmail());
        recipientRequest.setPhoneNo(user.getPhone());
        notificationRequestDto.setMessage(message);
        notificationRequestDto.setRecipient(Arrays.asList(recipientRequest));
        notificationService.emailNotificationRequest(notificationRequestDto);

        SmsRequest smsRequest = new SmsRequest();
        smsRequest.setMessage(message);
        smsRequest.setPhoneNumber(user.getPhone());

        notificationService.smsNotificationRequest(smsRequest);

        WhatsAppRequest whatsAppRequest = new WhatsAppRequest();
        whatsAppRequest.setMessage(message);
        whatsAppRequest.setPhoneNumber(user.getPhone());
        whatsAppService.whatsAppNotification(whatsAppRequest);

    }

}
