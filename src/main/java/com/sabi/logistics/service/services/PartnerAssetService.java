package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.DriverAssetDto;
import com.sabi.logistics.core.dto.request.PartnerAssetRequestDto;
import com.sabi.logistics.core.dto.response.DriverAssetResponseDto;
import com.sabi.logistics.core.dto.response.PartnerAssetResponseDto;
import com.sabi.logistics.core.enums.DriverType;
import com.sabi.logistics.core.models.*;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@SuppressWarnings("ALL")
@Service
@Slf4j
public class PartnerAssetService {
    private final PartnerAssetRepository partnerAssetRepository;
    private final PartnerAssetTypeRepository partnerAssetTypeRepository;
    private final ModelMapper mapper;
    private final Validations validations;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ColorRepository colorRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PartnerAssetPictureRepository partnerAssetPictureRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private AssetTypePropertiesRepository assetTypePropertiesRepository;

    @Autowired
    private DriverAssetRepository driverAssetRepository;
    @Autowired
    private DriverAssetService driverAssetService;

    public PartnerAssetService(PartnerAssetRepository partnerAssetRepository, PartnerAssetTypeRepository partnerAssetTypeRepository, ModelMapper mapper, Validations validations) {
        this.partnerAssetRepository = partnerAssetRepository;
        this.partnerAssetTypeRepository = partnerAssetTypeRepository;
        this.mapper = mapper;
        this.validations = validations;
    }

    public PartnerAssetResponseDto createPartnerAsset(PartnerAssetRequestDto request) {
        log.info("Request ::::::::::::::::::::::::::::::::: {} " + request);
        validations.validatePartnerAsset(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerAsset partnerAsset = mapper.map(request, PartnerAsset.class);
        PartnerAsset PartnerAssetExists = partnerAssetRepository.findByPlateNo(request.getPlateNo());
        DriverAssetResponseDto responseDto = new DriverAssetResponseDto();
        if (PartnerAssetExists != null) {
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " partnerAsset already exist");
        }
        PartnerAssetType partnerAssetType = partnerAssetTypeRepository.getOne(request.getPartnerAssetTypeId());
        Partner partner = partnerRepository.getOne(partnerAssetType.getPartnerId());
        AssetTypeProperties assetTypeProperties = assetTypePropertiesRepository.getOne(partnerAssetType.getAssetTypeId());
        partnerAsset.setPartnerName(partner.getName());

        Brand brand = brandRepository.getOne(request.getBrandId());
        Color color = colorRepository.getOne(request.getColorId());
        Driver driver = driverRepository.findByUserId(request.getDriverId());
        Driver driver2 = driverRepository.findByUserId(request.getDriverAssistantId());
        User user = userRepository.getOne(driver.getUserId());
        User user2 = userRepository.getOne(driver2.getUserId());

        partnerAsset.setDriverId(driver.getId());
        partnerAsset.setDriverAssistantId(driver2.getId());

        partnerAsset.setBrandName(brand.getName());
        partnerAsset.setColorName(color.getName());
        partnerAsset.setDriverName(user.getLastName() + " " + user.getFirstName());

        partnerAsset.setAssetTypeName(assetTypeProperties.getName());
        partnerAsset.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());
        partnerAsset.setCreatedBy(userCurrent.getId());
        partnerAsset.setIsActive(true);
        partnerAsset = partnerAssetRepository.save(partnerAsset);
        log.info("Request kKKKKKK ::::::::::::::::::::::::::::::::: {} " + partnerAsset);

        log.debug("Create new PartnerAsset - {}" + new Gson().toJson(partnerAsset));
        PartnerAssetResponseDto partnerAssetResponseDto = mapper.map(partnerAsset, PartnerAssetResponseDto.class);
        partnerAssetResponseDto.setBrandName(brand.getName());
        partnerAssetResponseDto.setColorName(color.getName());
        partnerAssetResponseDto.setDriverName(user.getLastName() + " " + user.getFirstName());
        partnerAssetResponseDto.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());
        partnerAssetResponseDto.setAssetTypeName(assetTypeProperties.getName());
        partnerAssetResponseDto.setAssetTypeName(partner.getName());
        log.info("Check assset ::::::::::::::::::::::::::::::::::::::::::::::::::::::: " + partnerAsset);
        DriverAsset driverAssetDto = new DriverAsset();
        DriverAssetDto processDriver = new DriverAssetDto();
        DriverAsset saveDrivedAsset = new DriverAsset();

        driverAssetDto = driverAssetRepository.findByDriverIdAndPartnerAssetId(driverAssetDto.getDriverId(), driverAssetDto.getPartnerAssetId());
        if (driverAssetDto == null) {
            processDriver.setPartnerAssetId(partnerAsset.getId());
            processDriver.setDriverType(DriverType.DRIVER);
            processDriver.setDriverId(partnerAsset.getDriverId());
            processDriver.setAssestTypeName(partnerAsset.getAssetTypeName());
           responseDto =  driverAssetService.createDriverAsset(processDriver);
        }
        saveDrivedAsset = driverAssetRepository.findByPartnerAssetIdAndId(responseDto.getPartnerAssetId(),responseDto.getId());
        if (saveDrivedAsset == null){
            processDriver.setPartnerAssetId(partnerAsset.getId());
            processDriver.setDriverType(DriverType.DRIVER_ASSISTANT);
            processDriver.setDriverId(partnerAsset.getDriverId());
            processDriver.setAssestTypeName(partnerAsset.getAssetTypeName());
            driverAssetService.createDriverAsset(processDriver);
        }
        return partnerAssetResponseDto;




    }

    public PartnerAssetResponseDto updatePartnerAsset(PartnerAssetRequestDto request) {
        validations.validatePartnerAsset(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerAsset partnerAsset = partnerAssetRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partnerAsset Id does not exist!"));
        mapper.map(request, partnerAsset);
        partnerAsset.setUpdatedBy(userCurrent.getId());
        if (request.getDriverId() != null || request.getDriverAssistantId() != null) {
            Driver driver = driverRepository.findByUserId(request.getDriverId());
            Driver driver2 = driverRepository.findByUserId(request.getDriverAssistantId());
            User user = userRepository.getOne(driver.getUserId());
            User user2 = userRepository.getOne(driver2.getUserId());

            partnerAsset.setDriverId(driver.getId());
            partnerAsset.setDriverAssistantId(driver2.getId());
            partnerAsset.setDriverName(user.getLastName() + " " + user.getFirstName());
            partnerAsset.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());
        }
            partnerAssetRepository.save(partnerAsset);
        log.debug("partnerAsset record updated - {}"+ new Gson().toJson(partnerAsset));
        PartnerAssetResponseDto partnerAssetResponseDto = mapper.map(partnerAsset, PartnerAssetResponseDto.class);

        if ((request.getPartnerAssetTypeId() != null || request.getBrandId() != null || request.getColorId() != null )) {
            PartnerAssetType partnerAssetType = partnerAssetTypeRepository.getOne(request.getPartnerAssetTypeId());
            Partner partner = partnerRepository.getOne(partnerAssetType.getPartnerId());
            AssetTypeProperties assetTypeProperties = assetTypePropertiesRepository.getOne(partnerAssetType.getAssetTypeId());
            Brand brand = brandRepository.getOne(request.getBrandId());
            Color color = colorRepository.getOne(request.getColorId());
            partnerAssetResponseDto.setPartnerName(partner.getName());
            partnerAssetResponseDto.setBrandName(brand.getName());
            partnerAssetResponseDto.setColorName(color.getName());
//            partnerAssetResponseDto.setDriverName(user.getLastName() + " " + user.getFirstName());
//            partnerAssetResponseDto.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());
            partnerAssetResponseDto.setAssetTypeName(assetTypeProperties.getName());

        }
        log.info("Check assset ::::::::::::::::::::::::::::::::::::::::::::::::::::::: " +partnerAsset);
        DriverAsset driverAssetDto = new DriverAsset();
        DriverAssetDto processDriver = new DriverAssetDto();
        driverAssetDto = driverAssetRepository.findByPartnerAssetId(partnerAsset.getId());
        log.info("Check assset ::::::::::::::::::::::::::::::::::::::::::::::::::::::: " +driverAssetDto);
        processDriver.setPartnerAssetId(partnerAsset.getId());
        processDriver.setDriverType(DriverType.DRIVER);
        processDriver.setDriverId(partnerAsset.getDriverId());
        processDriver.setAssestTypeName(partnerAsset.getAssetTypeName());
        driverAssetDto = driverAssetRepository.findByPartnerAssetId(partnerAsset.getId());
        processDriver.setId(driverAssetDto.getId());
        log.info("Driver assset ::::::::::::::::::::::::::::::::::::::::::::::::::::::: " + processDriver);
        driverAssetService.updateDriverAsset(processDriver);
        return partnerAssetResponseDto;
    }


    public PartnerAssetResponseDto findPartnerAsset(Long id){
        PartnerAsset partnerAsset  = partnerAssetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested PartnerAsset Id does not exist!"));
        PartnerAssetResponseDto partnerAssetResponseDto = mapper.map(partnerAsset, PartnerAssetResponseDto.class);
        partnerAssetResponseDto.setPartnerAssetPictures(getAllPartnerAssetPicture(id));

        PartnerAssetType partnerAssetType = partnerAssetTypeRepository.getOne(partnerAssetResponseDto.getPartnerAssetTypeId());
        Partner partner = partnerRepository.getOne(partnerAssetType.getPartnerId());
        AssetTypeProperties assetTypeProperties = assetTypePropertiesRepository.getOne(partnerAssetType.getAssetTypeId());

        Brand brand = brandRepository.getOne(partnerAssetResponseDto.getBrandId());
        Color color = colorRepository.getOne(partnerAssetResponseDto.getColorId());
        Driver driver = driverRepository.getOne(partnerAssetResponseDto.getDriverId());
        Driver driver2 = driverRepository.getOne(partnerAssetResponseDto.getDriverAssistantId());
        User user = userRepository.getOne(driver.getUserId());
        User user2 = userRepository.getOne(driver2.getUserId());

        partnerAssetResponseDto.setPartnerName(partner.getName());
        partnerAssetResponseDto.setBrandName(brand.getName());
        partnerAssetResponseDto.setColorName(color.getName());
        partnerAssetResponseDto.setDriverName(user.getLastName() + " " + user.getFirstName());
        partnerAssetResponseDto.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());
        partnerAssetResponseDto.setAssetTypeName(assetTypeProperties.getName());


        return partnerAssetResponseDto;
    }


    public Page<PartnerAsset> findAll(String name,Long brandId, String status, Long driverId,Long partnerId, Long partnerAssetTypeId, Boolean isActive, PageRequest pageRequest ){
        Page<PartnerAsset> partnerAssets = partnerAssetRepository.findPartnerAsset(name,brandId,status,driverId,partnerId,partnerAssetTypeId,isActive,pageRequest);
        if(partnerAssets == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        partnerAssets.getContent().forEach(asset ->{
            PartnerAsset partnerAsset = partnerAssetRepository.getOne(asset.getId());
            PartnerAssetType partnerAssetType = partnerAssetTypeRepository.getOne(partnerAsset.getPartnerAssetTypeId());
            Partner partner = partnerRepository.getOne(partnerAssetType.getPartnerId());
            AssetTypeProperties assetTypeProperties = assetTypePropertiesRepository.getOne(partnerAssetType.getAssetTypeId());
            asset.setPartnerName(partner.getName());
            asset.setAssetTypeName(assetTypeProperties.getName());

            Brand brand = brandRepository.getOne(partnerAsset.getBrandId());
            asset.setBrandName(brand.getName());

            Color color = colorRepository.getOne(partnerAsset.getColorId());
            asset.setColorName(color.getName());

            Driver driver = driverRepository.getOne(partnerAsset.getDriverId());
            User user = userRepository.getOne(driver.getUserId());
            asset.setDriverName(user.getLastName() + " " + user.getFirstName());

            Driver driver2 = driverRepository.getOne(partnerAsset.getDriverAssistantId());
            User user2 = userRepository.getOne(driver2.getUserId());
            asset.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());

        });

        return partnerAssets;

    }



    public void enableDisEnableState (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerAsset partnerAsset  = partnerAssetRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested PartnerAsset Id does not exist!"));
        partnerAsset.setIsActive(request.isActive());
        partnerAsset.setUpdatedBy(userCurrent.getId());
        partnerAssetRepository.save(partnerAsset);

    }


    public List<PartnerAsset> getAll(Long partnerId,Boolean isActive){
        List<PartnerAsset> partnerAssets = partnerAssetRepository.findByIsActiveAndId(partnerId,isActive);

        for (PartnerAsset asset : partnerAssets) {

            PartnerAssetType partnerAssetType = partnerAssetTypeRepository.getOne(asset.getPartnerAssetTypeId());
            Partner partner = partnerRepository.getOne(partnerAssetType.getPartnerId());
            AssetTypeProperties assetTypeProperties = assetTypePropertiesRepository.getOne(partnerAssetType.getAssetTypeId());

            Brand brand = brandRepository.getOne(asset.getBrandId());
            Color color = colorRepository.getOne(asset.getColorId());
            Driver driver = driverRepository.getOne(asset.getDriverId());
            User user = userRepository.getOne(driver.getUserId());
            Driver driver2 = driverRepository.getOne(asset.getDriverAssistantId());
            User user2 = userRepository.getOne(driver2.getUserId());
            asset.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());


            asset.setPartnerName(partner.getName());
            asset.setBrandName(brand.getName());
            asset.setColorName(color.getName());
            asset.setDriverName(user.getLastName() + " " + user.getFirstName());
            asset.setAssetTypeName(assetTypeProperties.getName());

        }

        return partnerAssets;

    }

    public List<PartnerAssetPicture> getAllPartnerAssetPicture(Long partnerAssetId){
        List<PartnerAssetPicture> partnerAssetPictures = partnerAssetPictureRepository.findByPartnerAssetId(partnerAssetId);
        return partnerAssetPictures;

    }
}
