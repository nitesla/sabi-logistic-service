package com.sabi.logistics.service.services;

import com.sabi.framework.models.User;
import com.sabi.logistics.core.dto.request.AdminAuthDto;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;

public interface AdminUserService {

    public ResponseEntity<? extends Object> createOrLoginAdmin(AdminAuthDto adminAuthDto, HttpServletRequest request);

    public void dispatchNotificationsToAdmin(User user, String message);
}
