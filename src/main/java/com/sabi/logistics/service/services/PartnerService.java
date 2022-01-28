package com.sabi.logistics.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.ChangePasswordDto;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.BadRequestException;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.PreviousPasswords;
import com.sabi.framework.models.User;
import com.sabi.framework.models.UserRole;
import com.sabi.framework.repositories.PreviousPasswordRepository;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.repositories.UserRoleRepository;
import com.sabi.framework.service.AuditTrailService;
import com.sabi.framework.service.NotificationService;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.AuditTrailFlag;
import com.sabi.framework.utils.Constants;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.framework.utils.Utility;
import com.sabi.logistics.core.dto.request.CompleteSignupRequest;
import com.sabi.logistics.core.dto.request.PartnerDto;
import com.sabi.logistics.core.dto.request.PartnerSignUpDto;
import com.sabi.logistics.core.dto.response.CompleteSignUpResponse;
import com.sabi.logistics.core.dto.response.PartnerActivationResponse;
import com.sabi.logistics.core.dto.response.PartnerResponseDto;
import com.sabi.logistics.core.dto.response.PartnerSignUpResponseDto;
import com.sabi.logistics.core.models.*;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@SuppressWarnings("ALL")
@Slf4j
@Service
public class PartnerService {

    @Autowired
    private PasswordEncoder passwordEncoder;
    private PartnerRepository repository;
    private PartnerAssetTypeRepository partnerAssetTypeRepository;
    private PartnerCategoriesRepository partnerCategoriesRepository;
    private PartnerLocationRepository partnerLocationRepository;
    private UserRepository userRepository;
    private PreviousPasswordRepository previousPasswordRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;
    private NotificationService notificationService;
    private final PartnerUserRepository partnerUserRepository;
    private LGARepository lgaRepository;
    private final AuditTrailService auditTrailService;
    private final StateRepository stateRepository;
    private final UserRoleRepository userRoleRepository;


    public PartnerService(PartnerRepository repository,PartnerAssetTypeRepository partnerAssetTypeRepository,
                          PartnerCategoriesRepository partnerCategoriesRepository,PartnerLocationRepository partnerLocationRepository,
                          UserRepository userRepository,PreviousPasswordRepository previousPasswordRepository,
                          ModelMapper mapper, ObjectMapper objectMapper,
                          Validations validations,NotificationService notificationService,
                          PartnerUserRepository partnerUserRepository,LGARepository lgaRepository,AuditTrailService auditTrailService,
                          StateRepository stateRepository,UserRoleRepository userRoleRepository) {
        this.repository = repository;
        this.partnerAssetTypeRepository = partnerAssetTypeRepository;
        this.partnerCategoriesRepository = partnerCategoriesRepository;
        this.partnerLocationRepository = partnerLocationRepository;
        this.userRepository = userRepository;
        this.previousPasswordRepository = previousPasswordRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
        this.notificationService = notificationService;
        this.partnerUserRepository = partnerUserRepository;
        this.lgaRepository = lgaRepository;
        this.auditTrailService = auditTrailService;
        this.stateRepository = stateRepository;
        this.userRoleRepository = userRoleRepository;
    }




    public PartnerSignUpResponseDto partnerSignUp(PartnerSignUpDto request,HttpServletRequest request1) {
        validations.validatePartner(request);
        User user = mapper.map(request,User.class);

        User exist = userRepository.findByEmailOrPhone(request.getEmail(),request.getPhone());
        if(exist !=null && exist.getPasswordChangedOn()== null){

            Partner partnerExist = repository.findByUserId(exist.getId());
            if(partnerExist !=null){
          PartnerSignUpResponseDto partnerSignUpResponseDto= PartnerSignUpResponseDto.builder()
                  .id(exist.getId())
                  .email(exist.getEmail())
                  .firstName(exist.getFirstName())
                  .lastName(exist.getLastName())
                  .phone(exist.getPhone())
                  .username(exist.getUsername())
                  .partnerId(partnerExist.getId())
                  .build();
          return partnerSignUpResponseDto;
            }else {
                throw new BadRequestException(CustomResponseCode.BAD_REQUEST, " Partner id does not exist");
            }

        }else if(exist !=null && exist.getPasswordChangedOn() !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Partner user already exist");
        }
        String password = request.getPassword();
        user.setPassword(passwordEncoder.encode(password));
        user.setUserCategory(Constants.OTHER_USER);
        user.setUsername(request.getEmail());
        user.setLoginAttempts(0);
        user.setCreatedBy(0l);
        user.setIsActive(false);
        user = userRepository.save(user);
        log.debug("Create new agent user - {}"+ new Gson().toJson(user));

        UserRole userRole = UserRole.builder()
                .userId(user.getId())
                .roleId(user.getRoleId())
                .createdDate(LocalDateTime.now())
                .build();
        userRoleRepository.save(userRole);

        PreviousPasswords previousPasswords = PreviousPasswords.builder()
                .userId(user.getId())
                .password(user.getPassword())
                .createdDate(LocalDateTime.now())
                .build();
        previousPasswordRepository.save(previousPasswords);

        Partner savePartner = new Partner();
        savePartner.setName(request.getName());
        savePartner.setUserId(user.getId());
        savePartner.setRegistrationToken(Utility.registrationCode("HHmmss"));
        savePartner.setRegistrationTokenExpiration(Utility.expiredTime());
        savePartner.setIsActive(false);
        savePartner.setCreatedBy(user.getId());

        Partner partnerResponse= repository.save(savePartner);
        log.debug("Create new partner  - {}"+ new Gson().toJson(savePartner));

        PartnerUser partnerUser = new PartnerUser();
        partnerUser.setPartnerId(partnerResponse.getId());
        partnerUser.setUserId(user.getId());
        partnerUser.setCreatedBy(0l);
        partnerUser.setIsActive(false);
        partnerUserRepository.save(partnerUser);

        PartnerSignUpResponseDto response = PartnerSignUpResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .name(partnerResponse.getName())
                .partnerId(partnerResponse.getId())
                .build();

        auditTrailService
                .logEvent(response.getUsername(),
                        "SignUp partner :" + response.getUsername(),
                        AuditTrailFlag.SIGNUP,
                        " Sign up partner Request for:" + user.getFirstName() + " " + user.getLastName() + " " + user.getEmail()
                        , 1, Utility.getClientIp(request1));
        return response;
    }



    public CompleteSignUpResponse completeSignUp(CompleteSignupRequest request) {
        validations.validatePartnerProperties(request);
        Partner partner = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partner Id does not exist!"));
        mapper.map(request, partner);

        partner.setUpdatedBy(partner.getUserId());
        partner.setIsActive(true);
        repository.save(partner);
        log.debug("complete signup  - {}"+ new Gson().toJson(partner));

        List<PartnerCategories> categories = new ArrayList<>();
        request.getCategories().forEach(p -> {
            PartnerCategories tran = PartnerCategories.builder()
                    .categoryId(p.getCategoryId())
                    .build();
            tran.setPartnerId(partner.getId());
            tran.setCreatedBy(partner.getUserId());
            tran.setIsActive(true);
            log.info(" category details " + tran);
            partnerCategoriesRepository.save(tran);
            categories.add(tran);

        });
          List<PartnerAssetType> assetTypes = new ArrayList<>();
          request.getAssets().forEach(a -> {
              PartnerAssetType asset = PartnerAssetType.builder()
                      .assetTypeId(a.getAssetTypeId())
                      .total(a.getTotal())
                      .build();
              asset.setPartnerId(partner.getId());
              asset.setCreatedBy(partner.getUserId());
              asset.setIsActive(true);
              log.info(" Asset type details " + asset);
              partnerAssetTypeRepository.save(asset);
              assetTypes.add(asset);
          });

          List<PartnerLocation> location = new ArrayList<>();
          request.getLocations().forEach(l -> {
              PartnerLocation partnerLocation = PartnerLocation.builder()
                      .stateId(l.getStateId())
                      .wareHouses(l.getWareHouses())
                      .build();
              partnerLocation.setPartnerId(partner.getId());
              partnerLocation.setCreatedBy(partner.getUserId());
              partnerLocation.setIsActive(true);
              log.info(" location details " + partnerLocation);
              partnerLocationRepository.save(partnerLocation);
              location.add(partnerLocation);
          });
        User user = userRepository.getOne(partner.getUserId());
        user.setIsActive(true);
        user.setUpdatedBy(partner.getUserId());
        user.setPasswordChangedOn(LocalDateTime.now());
        userRepository.save(user);

        PartnerUser partnerUser = partnerUserRepository.findByUserId(user.getId());
         partnerUser.setIsActive(true);
        partnerUserRepository.save(partnerUser);

        CompleteSignUpResponse response = CompleteSignUpResponse.builder()
                .partnerId(partner.getId())
                .email(partner.getEmail())
                .name(partner.getName())
                .phone(partner.getPhone())
                .address(partner.getAddress())
                .registrationDate(partner.getRegistrationDate())
                .userId(partner.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userEmail(user.getEmail())
                .userName(user.getUsername())
                .userPhone(user.getPhone())
                .build();
        return response;

    }




    public PartnerActivationResponse partnerPasswordActivation(ChangePasswordDto request) {

        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested user id does not exist!"));
        mapper.map(request, user);

        String password = request.getPassword();
        user.setPassword(passwordEncoder.encode(password));
        user.setPasswordChangedOn(LocalDateTime.now());
        user = userRepository.save(user);

        PreviousPasswords previousPasswords = PreviousPasswords.builder()
                .userId(user.getId())
                .password(user.getPassword())
                .createdDate(LocalDateTime.now())
                .build();
        previousPasswordRepository.save(previousPasswords);

        PartnerUser partner = partnerUserRepository.findByUserId(user.getId());
          partner.setIsActive(true);
        partnerUserRepository.save(partner);


        PartnerActivationResponse response = PartnerActivationResponse.builder()
                .userId(user.getId())
                .partnerId(partner.getPartnerId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .build();

        return response;
    }





    public PartnerResponseDto createPartnerProperties(PartnerDto request) {
//        validations.validatePartnerProperties(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Partner partnerProperties = mapper.map(request,Partner.class);
        Partner exist = repository.findByName(request.getName());
        if(exist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " partner properties already exist");
        }
        partnerProperties.setCreatedBy(userCurrent.getId());
        partnerProperties.setIsActive(true);
        partnerProperties = repository.save(partnerProperties);
        log.debug("Create new partner asset - {}"+ new Gson().toJson(partnerProperties));
        return mapper.map(partnerProperties, PartnerResponseDto.class);
    }


    public PartnerResponseDto updatePartnerProperties(PartnerDto request,HttpServletRequest request1) {
        validations.validatePartnerUpdate(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Partner partnerProperties = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partner properties Id does not exist!"));
        mapper.map(request, partnerProperties);
        partnerProperties.setUpdatedBy(userCurrent.getId());
        repository.save(partnerProperties);
        log.debug("partner asset record updated - {}"+ new Gson().toJson(partnerProperties));
        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Update partner by username:" + userCurrent.getUsername(),
                        AuditTrailFlag.UPDATE,
                        " Update partner Request for:" + partnerProperties.getId() ,1, Utility.getClientIp(request1));
        return mapper.map(partnerProperties, PartnerResponseDto.class);
    }


    public PartnerResponseDto findPartnerAsset(Long id){
        Partner partnerProperties  = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partner properties Id does not exist!"));
        LGA lga = lgaRepository.findLGAById(partnerProperties.getLgaId());

        State state = stateRepository.getOne(lga.getStateId());

        partnerProperties.setLga(lga.getName());
        partnerProperties.setState(state.getName());

        return mapper.map(partnerProperties,PartnerResponseDto.class);
    }


    public Page<Partner> findAll(String name, PageRequest pageRequest ){
        Page<Partner> partnerProperties = repository.findPartnersProperties(name,pageRequest);
        if(partnerProperties == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        partnerProperties.getContent().forEach(partner ->{
            LGA lga = lgaRepository.findLGAById(partner.getLgaId());
            partner.setLga(lga.getName());
        });
        return partnerProperties;

    }



    public void enableDisEnable (EnableDisEnableDto request,HttpServletRequest request1){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Partner partnerProperties = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partner properties Id does not exist!"));
        partnerProperties.setIsActive(request.isActive());
        partnerProperties.setUpdatedBy(userCurrent.getId());

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Disable/Enable partner by :" + userCurrent.getUsername() ,
                        AuditTrailFlag.UPDATE,
                        " Disable/Enable partner Request for:" +  partnerProperties.getId()
                                + " " +  partnerProperties.getName(),1, Utility.getClientIp(request1));
        repository.save(partnerProperties);

    }


    public List<Partner> getAll(Boolean isActive){
        List<Partner> partnerProperties = repository.findByIsActive(isActive);
        for (Partner part : partnerProperties
                ) {
            LGA lga = lgaRepository.findLGAById(part.getLgaId());
            part.setLga(lga.getName());

        }
        return partnerProperties;

    }
}
