package com.sabi.logistics.service.services;

import com.sabi.framework.models.User;

import java.util.concurrent.CompletableFuture;

public interface GeneralNotificationService {
    public void dispatchNotificationsToUser(User user,  String message);

}
