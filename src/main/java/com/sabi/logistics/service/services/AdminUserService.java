package com.sabi.logistics.service.services;

import com.sabi.framework.models.User;
import com.sabi.logistics.core.dto.response.admin.AccessTokenWithUserDetails;
import com.sabi.logistics.core.dto.request.AdminAuthDto;

public interface AdminUserService {

    public AccessTokenWithUserDetails createOrLoginAdmin(AdminAuthDto adminAuthDto);

    public void dispatchNotificationsToAdmin(User user, String message);
}
