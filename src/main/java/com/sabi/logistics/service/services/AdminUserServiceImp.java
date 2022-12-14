package com.sabi.logistics.service.services;

import com.sabi.framework.dto.requestDto.LoginRequest;
import com.sabi.framework.exceptions.*;
import com.sabi.framework.globaladminintegration.GlobalService;
import com.sabi.framework.helpers.API;
import com.sabi.framework.models.*;
import com.sabi.framework.repositories.PreviousPasswordRepository;
import com.sabi.framework.repositories.RoleRepository;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.repositories.UserRoleRepository;
import com.sabi.framework.security.AuthenticationWithToken;
import com.sabi.framework.service.*;
import com.sabi.framework.utils.AuditTrailFlag;
import com.sabi.framework.utils.Constants;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.framework.utils.Utility;
import com.sabi.logistics.core.dto.request.AdminAuthDto;
import com.sabi.logistics.core.dto.response.admin.AccessTokenWithUserDetails;
import com.sabi.logistics.core.dto.response.admin.AdminPermission;
import com.sabi.logistics.core.dto.response.admin.UserInfoResponse;
import com.sabi.logistics.core.integrations.response.admin.UserResponseGlobalAdmin;
import com.sabi.logistics.service.helper.Validations;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AdminUserServiceImp implements AdminUserService {

    private final UserRepository userRepository;

    private final PreviousPasswordRepository previousPasswordRepository;

    private final API api;

    private final PasswordEncoder passwordEncoder;

    private final RoleRepository roleRepository;

    private final AuditTrailService auditTrailService;

    private final ModelMapper modelMapper;

    private final UserRoleRepository userRoleRepository;

    private final UserService userService;

    private final Validations validations;

    private final GlobalService globalService;

    private final TokenService tokenService;

    private final GeneralNotificationService generalNotificationService;

    @Value("${globalAdmin.auth.url}")
    private String globalAdminAuthUrl;

    @Value("${login.attempts}")
    private int loginAttempts;


    public AdminUserServiceImp(UserRepository userRepository, PreviousPasswordRepository previousPasswordRepository, API api, NotificationService notificationService, WhatsAppService whatsAppService, PasswordEncoder passwordEncoder, RoleRepository roleRepository, AuditTrailService auditTrailService, ModelMapper modelMapper, UserRoleRepository userRoleRepository, UserService userService, Validations validations, GlobalService globalService, TokenService tokenService, GeneralNotificationService generalNotificationService) {
        this.userRepository = userRepository;
        this.previousPasswordRepository = previousPasswordRepository;
        this.api = api;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.auditTrailService = auditTrailService;
        this.modelMapper = modelMapper;
        this.userRoleRepository = userRoleRepository;
        this.userService = userService;
        this.validations = validations;
        this.globalService = globalService;
        this.tokenService = tokenService;
        this.generalNotificationService = generalNotificationService;
    }

    private ResponseEntity<? extends Object> loginAdmin(UserInfoResponse userInfoResponse){
        User user = userRepository.findByEmail(userInfoResponse.getEmail());

        if (user!= null){
            user.setLoginStatus(true);
            user.setLastLogin(LocalDateTime.now());
            AccessTokenWithUserDetails accessTokenWithUserDetails = this.modelMapper.map(userInfoResponse, AccessTokenWithUserDetails.class);
            accessTokenWithUserDetails.setGlobalAdminToken(userInfoResponse.getToken());
            accessTokenWithUserDetails.setPermissions(userInfoResponse.getPermissions());
            accessTokenWithUserDetails.setLastLogin(LocalDateTime.now());
            accessTokenWithUserDetails.setGlobalAdminUserId(userInfoResponse.getUserId());
            //User successfully logged in and known
            //Generate token, sets it to security context and generates permission/access list
            String newToken = "Bearer "+this.tokenService.generateNewToken(); //application specific token
            accessTokenWithUserDetails.setAccessToken(newToken);// logistics generated appToken.
            accessTokenWithUserDetails.setUserId(user.getId());
            accessTokenWithUserDetails.setTokenExpiry(userService.getSessionExpiry());
            return this.getPermissionsAccessListAndSetToken(user, newToken, accessTokenWithUserDetails);
        }
        else {
            log.info("The login status =={}",user.isLoginStatus());
            chekLoginAttemptsAndLockAccount(user);
            throw new UnauthorizedException(CustomResponseCode.UNAUTHORIZED, "Invalid Login Details: Username does not exist");
        }
    }

    private void chekLoginAttemptsAndLockAccount(User user) {
        if (loginAttempts <= user.getLoginAttempts() || user.getLastLogin()!= null){
            //lock Account
            user.setAccountNonLocked(false);
            user.setLockedDate(new Date());
            userService.lockLogin(user.getId());
            throw new LockedException(CustomResponseCode.LOCKED_EXCEPTION,"Your account is locked. It must be unlocked before you can login");
        }
        userService.updateFailedLogin(user.getId());
    }

    private ResponseEntity<AccessTokenWithUserDetails> getPermissionsAccessListAndSetToken(User user, String newToken, AccessTokenWithUserDetails accessTokenWithUserDetails) {

        List<AdminPermission> adminPermissionList = accessTokenWithUserDetails.getPermissions(); // Here is an admin permissions
        AuthenticationWithToken authenticationWithToken = new AuthenticationWithToken
                (user,null, AuthorityUtils.commaSeparatedStringToAuthorityList(adminPermissionList.stream().toString()));
        authenticationWithToken.setToken(newToken);
        tokenService.store(newToken,authenticationWithToken);
        SecurityContextHolder.getContext().setAuthentication(authenticationWithToken);
        userService.updateLogin(user.getId());
        return ResponseEntity.ok().body(accessTokenWithUserDetails);
        //}
    }


    @Override
    public ResponseEntity<? extends Object> createOrLoginAdmin(AdminAuthDto adminAuthDto, HttpServletRequest request) {

        this.validations.validateAdminAuth(adminAuthDto);
        // call the global token api first to authenticate the user
        Map<String,String> requestParams = new HashMap();
        requestParams.put("applicationCode",adminAuthDto.getApplicationCode());
        requestParams.put("authKey",adminAuthDto.getAuthKey());
        requestParams.put("userId", String.valueOf(adminAuthDto.getAuthKey()));
        StringBuilder urlPathBuilder = new StringBuilder(globalAdminAuthUrl);
        urlPathBuilder.append("?applicationCode=").append(adminAuthDto.getApplicationCode());
        urlPathBuilder.append("&authKey=").append(adminAuthDto.getAuthKey());
        urlPathBuilder.append("&userId=").append(adminAuthDto.getUserId());
        UserResponseGlobalAdmin response = this.api.get(urlPathBuilder.toString(),UserResponseGlobalAdmin.class,requestParams);
        if (response.getCode().equalsIgnoreCase("200")){
            UserInfoResponse userInfoResponse = (UserInfoResponse)response.getData();
            //Is the user authenticated on globalAdmin
            if (userInfoResponse != null){
                User user = userRepository.findByEmail(userInfoResponse.getEmail());
                if (user != null){

                    //The user already exists. Just Login
                    // Log the user in
                    return this.loginAdmin(userInfoResponse);
                }
                //The user doesn't exist yet, register him/her before login
                else {
                    user = this.modelMapper.map(userInfoResponse,User.class);
                    user = this.createAdminUser(userInfoResponse,user, request);
                    return this.loginAdmin(userInfoResponse);

                }
            }
            else {
                throw new ConflictException(CustomResponseCode.INTERNAL_SERVER_ERROR," Internal error occured. Contact Administrator");
            }

        }
        else {
            if (response.getCode().equalsIgnoreCase("401"))
            throw new UnauthorizedException(CustomResponseCode.UNAUTHORIZED,response.getDescription());
            else if (response.getCode().equals("400"))
                throw new BadRequestException(CustomResponseCode.BAD_REQUEST,response.getDescription());
            else if (response.getCode().equals("404"))
                throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,response.getDescription());
            else if (response.getCode().equals("500"))
                throw new ConflictException(CustomResponseCode.INTERNAL_SERVER_ERROR,response.getDescription());
            else
                throw new ConflictException(response.getCode(),response.getDescription());

        }
    }

    public User createAdminUser(UserInfoResponse userInfoResponse, User user, HttpServletRequest httpServletRequest){

        user.setGlobalAdminUserId(userInfoResponse.getUserId());
        user.setLoginAttempts(0);
        user.setIsActive(true);
        user.setCreatedBy(0L);
        user.setCreatedDate(LocalDateTime.now());
        user.setUpdatedDate(LocalDateTime.now());
        user.setUserCategory(Constants.ADMIN_USER);
        Role role = roleRepository.findByName("Admin Role");
        if (role == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"It seems like the admin role doesn't exist in the database");
        }
        user.setRoleId(role.getId());
        user = userRepository.save(user);
        UserRole userRole = UserRole.builder().userId(user.getId()).roleId(user.getRoleId()).build();
        userRoleRepository.save(userRole);
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(user.getUsername());
        this.auditTrailService.logEvent(user.getUsername(), "Create new user by :" + user.getUsername(), AuditTrailFlag.CREATE, " Create new user for:" + user.getFirstName() + " " + user.getUsername(), 1,
                Utility.getClientIp(httpServletRequest));
        return user;
    }
    @Override
    public void dispatchNotificationsToAdmin(User user, String message) {

        generalNotificationService.dispatchNotificationsToUser(user,message);

    }


}
