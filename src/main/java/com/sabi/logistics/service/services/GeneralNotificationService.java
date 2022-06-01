package com.sabi.logistics.service.services;

import com.sabi.framework.models.User;

import java.util.concurrent.CompletableFuture;

public interface GeneralNotificationService {
    public CompletableFuture<Boolean> dispatchNotificationsToUser(User user, String phone, String message);

}
