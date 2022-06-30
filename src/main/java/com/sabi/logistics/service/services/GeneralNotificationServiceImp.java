package com.sabi.logistics.service.services;

import com.sabi.framework.models.User;
import com.sabi.framework.notification.requestDto.NotificationRequestDto;
import com.sabi.framework.notification.requestDto.RecipientRequest;
import com.sabi.framework.notification.requestDto.SmsRequest;
import com.sabi.framework.notification.requestDto.WhatsAppRequest;
import com.sabi.framework.service.NotificationService;
import com.sabi.framework.service.WhatsAppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class GeneralNotificationServiceImp implements GeneralNotificationService{

    private final NotificationService notificationService;

    private final WhatsAppService whatsAppService;

    public GeneralNotificationServiceImp(NotificationService notificationService, WhatsAppService whatsAppService) {
        this.notificationService = notificationService;
        this.whatsAppService = whatsAppService;

    }

    @Async
    @Override
    public  void dispatchNotificationsToUser(User user,  String message) {
        log.info("Notifications Initiated... ");
        if (user != null){
            NotificationRequestDto notificationRequestDto = new NotificationRequestDto();
            notificationRequestDto.setEmail(true);
            RecipientRequest recipientRequest = new RecipientRequest();
            recipientRequest.setEmail(user.getEmail());
            recipientRequest.setPhoneNo(user.getPhone());
            notificationRequestDto.setMessage(message);
            notificationRequestDto.setRecipient(Arrays.asList(recipientRequest));
            notificationService.emailNotificationRequest(notificationRequestDto);
        }
        SmsRequest smsRequest = new SmsRequest();
        smsRequest.setMessage(message);
        smsRequest.setPhoneNumber(user.getPhone());

        notificationService.smsNotificationRequest(smsRequest);

        WhatsAppRequest whatsAppRequest = new WhatsAppRequest();
        whatsAppRequest.setMessage(message);
        whatsAppRequest.setPhoneNumber(user.getPhone());
        whatsAppService.whatsAppNotification(whatsAppRequest);

        log.info("Notifications successfully sent to User {} ",user.getFirstName());
        log.info("Notifications Ended... ");
    }
}
