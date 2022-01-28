package com.sabi.logistics.service.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.PreviousPasswords;
import com.sabi.framework.models.User;
import com.sabi.framework.repositories.PreviousPasswordRepository;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.repositories.UserRoleRepository;
import com.sabi.framework.service.NotificationService;
import com.sabi.framework.utils.Constants;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.ExternalPartnerSignUp;
import com.sabi.logistics.core.dto.response.ExternalDetailsResponse;
import com.sabi.logistics.core.dto.response.ExternalPartnerSignUpResponse;
import com.sabi.logistics.core.models.LGA;
import com.sabi.logistics.core.models.Partner;
import com.sabi.logistics.core.models.PartnerAssetType;
import com.sabi.logistics.core.models.PartnerUser;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.LGARepository;
import com.sabi.logistics.service.repositories.PartnerAssetTypeRepository;
import com.sabi.logistics.service.repositories.PartnerRepository;
import com.sabi.logistics.service.repositories.PartnerUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ALL")
@Slf4j
@Service
public class ExternalSignUpService {


    @Autowired
    private PasswordEncoder passwordEncoder;
    private PartnerRepository repository;
    private PartnerAssetTypeRepository partnerAssetTypeRepository;
    private UserRepository userRepository;
    private PreviousPasswordRepository previousPasswordRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;
    private NotificationService notificationService;
    private final PartnerUserRepository partnerUserRepository;
    private LGARepository lgaRepository;
    private final UserRoleRepository userRoleRepository;


    public ExternalSignUpService(PartnerRepository repository,PartnerAssetTypeRepository partnerAssetTypeRepository,
                          UserRepository userRepository,PreviousPasswordRepository previousPasswordRepository,
                          ModelMapper mapper, ObjectMapper objectMapper,
                          Validations validations,NotificationService notificationService,
                          PartnerUserRepository partnerUserRepository,LGARepository lgaRepository,UserRoleRepository userRoleRepository) {
        this.repository = repository;
        this.partnerAssetTypeRepository = partnerAssetTypeRepository;
        this.userRepository = userRepository;
        this.previousPasswordRepository = previousPasswordRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
        this.notificationService = notificationService;
        this.partnerUserRepository = partnerUserRepository;
        this.lgaRepository = lgaRepository;
        this.userRoleRepository = userRoleRepository;
    }



    public ExternalPartnerSignUpResponse externalSignUp(ExternalPartnerSignUp request) {
        User user = mapper.map(request,User.class);

        User exist = userRepository.findByEmailOrPhone(request.getEmail(),request.getPhone());
        if(exist !=null ){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Partner user already exist");
        }
        String password = request.getPassword();
        user.setPassword(passwordEncoder.encode(password));
        user.setUserCategory(Constants.OTHER_USER);
        user.setUsername(request.getEmail());
        user.setLoginAttempts(0);
        user.setCreatedBy(0l);
        user.setIsActive(true);
        user.setPasswordChangedOn(LocalDateTime.now());
        user = userRepository.save(user);
        log.debug("Create new agent user - {}"+ new Gson().toJson(user));



        PreviousPasswords previousPasswords = PreviousPasswords.builder()
                .userId(user.getId())
                .password(user.getPassword())
                .createdDate(LocalDateTime.now())
                .build();
        previousPasswordRepository.save(previousPasswords);

        Partner savePartner = new Partner();
        savePartner.setName(request.getName());
        savePartner.setUserId(user.getId());
        savePartner.setLgaId(request.getLgaId());
        savePartner.setIsActive(false);
        savePartner.setCreatedBy(user.getId());
        savePartner.setSupplierId(request.getSupplierId());

        Partner partnerResponse= repository.save(savePartner);
        log.debug("Create new partner  - {}"+ new Gson().toJson(savePartner));

        PartnerUser partnerUser = new PartnerUser();
        partnerUser.setPartnerId(partnerResponse.getId());
        partnerUser.setUserId(user.getId());
        partnerUser.setCreatedBy(0l);
        partnerUser.setIsActive(true);
        partnerUserRepository.save(partnerUser);

        List<PartnerAssetType> assetTypes = new ArrayList<>();
        request.getAssets().forEach(a -> {
            PartnerAssetType asset = PartnerAssetType.builder()
                    .assetTypeId(a.getAssetTypeId())
                    .total(a.getTotal())
                    .build();
            asset.setPartnerId(partnerResponse.getId());
            asset.setCreatedBy(partnerResponse.getUserId());
            asset.setIsActive(true);
            log.info(" Asset type details " + asset);
            partnerAssetTypeRepository.save(asset);
            assetTypes.add(asset);
        });

        ExternalPartnerSignUpResponse response = ExternalPartnerSignUpResponse.builder()
                .id(user.getId())
                .partnerId(partnerResponse.getId())
                .build();

        return response;


    }



    public ExternalDetailsResponse externalDetails(Long supplierId){
        Partner partner  = repository.findBySupplierId(supplierId);
        if(partner ==null)
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Supplier id does not exist");

        LGA lga = lgaRepository.getOne(partner.getLgaId());

        User user = userRepository.getOne(partner.getUserId());

        ExternalDetailsResponse externalDetailsResponse = ExternalDetailsResponse.builder()
                .address(partner.getAddress())
                .cac(partner.getCac())
                .createdBy(partner.getCreatedBy())
                .createdDate(partner.getCreatedDate())
                .email(partner.getEmail())
                .employeeCount(partner.getEmployeeCount())
                .name(partner.getName())
                .isRegistered(partner.isRegistered())
                .partnerId(partner.getId())
                .registrationDate(partner.getRegistrationDate())
                .supplierId(partner.getSupplierId())
                .webSite(partner.getWebSite())
                .lgaId(partner.getLgaId())
                .lgaName(lga.getName())
                .lastName(user.getLastName())
                .firstName(user.getFirstName())
                .userEmail(user.getEmail())
                .userPhone(user.getPhone())
                .userId(user.getId())
                .username(user.getUsername())
                .build();


        return externalDetailsResponse;
    }



}
