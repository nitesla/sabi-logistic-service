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

import javax.servlet.http.HttpServletRequest;
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

    public PartnerAssetResponseDto createPartnerAsset(PartnerAssetRequestDto request,HttpServletRequest request1) {
        log.info("Request ::::::::::::::::::::::::::::::::: {} " + request);
        validations.validatePartnerAsset(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PartnerAsset partnerAsset = mapper.map(request, PartnerAsset.class);
        PartnerAsset partnerAssetExists = partnerAssetRepository.findByPlateNo(request.getPlateNo());
        DriverAssetResponseDto responseDto = new DriverAssetResponseDto();
        if (partnerAssetExists != null) {
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Plate Number already exist");
        }
        PartnerAssetType partnerAssetType = partnerAssetTypeRepository.findPartnerAssetTypeById(request.getPartnerAssetTypeId());
        if (partnerAssetType == null) {
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid PartnerAssetType Id");
        }
        Partner partner = partnerRepository.findPartnerById(partnerAssetType.getPartnerId());
        if (partner == null) {
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid Partner Id");
        }
        AssetTypeProperties assetTypeProperties = assetTypePropertiesRepository.findAssetTypePropertiesById(partnerAssetType.getAssetTypeId());
        if (assetTypeProperties == null) {
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid AssetTypeProperties Id");
        }
        partnerAsset.setPartnerName(partner.getName());

        Brand brand = brandRepository.findBrandById(request.getBrandId());
        if (brand == null) {
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid Brand Id");
        }
        Color color = colorRepository.findColorById(request.getColorId());
        if (color == null) {
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Color Id");
        }
        Driver driver = driverRepository.findByUserId(request.getDriverUserId());
        if (driver == null) {
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Driver Id");
        }
        Driver driver2 = driverRepository.findByUserId(request.getDriverAssistantUserId());
        if (driver2 == null) {
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Driver Assistant Id");
        }
        User user = userRepository.getOne(driver.getUserId());
        User user2 = userRepository.getOne(driver2.getUserId());

        partnerAsset.setDriverId(driver.getId());
        partnerAsset.setDriverAssistantId(driver2.getId());

        partnerAsset.setAssetTypeName(assetTypeProperties.getName());
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
        log.info("Check assset ::::::::::::::::::::::::::::::::::::::::::::::::::::::: " + partnerAsset);
        DriverAsset driverAssetDto = new DriverAsset();
        DriverAssetDto processDriver = new DriverAssetDto();
        DriverAsset saveDrivedAsset = new DriverAsset();

        driverAssetDto = driverAssetRepository.findByDriverIdAndPartnerAssetId(partnerAsset.getDriverId(), partnerAsset.getId());
        if (driverAssetDto == null) {
            processDriver.setPartnerAssetId(partnerAsset.getId());
            processDriver.setDriverType(DriverType.DRIVER);
            processDriver.setDriverId(partnerAsset.getDriverId());
            processDriver.setAssestTypeName(partnerAsset.getAssetTypeName());
           responseDto =  driverAssetService.createDriverAsset(processDriver,request1);
        }
        saveDrivedAsset = driverAssetRepository.findByDriverIdAndPartnerAssetId(partnerAsset.getDriverAssistantId(),partnerAsset.getId());
        if (saveDrivedAsset == null){
            processDriver.setPartnerAssetId(partnerAsset.getId());
            processDriver.setDriverType(DriverType.DRIVER_ASSISTANT);
            processDriver.setDriverId(partnerAsset.getDriverAssistantId());
            processDriver.setAssestTypeName(partnerAsset.getAssetTypeName());
            driverAssetService.createDriverAsset(processDriver,request1);
        }
        return partnerAssetResponseDto;




    }

    public PartnerAssetResponseDto updatePartnerAsset(PartnerAssetRequestDto request,HttpServletRequest request1) {
        validations.validatePartnerAsset(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DriverAssetResponseDto responseDto = new DriverAssetResponseDto();
        PartnerAsset partnerAsset = partnerAssetRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested partnerAsset Id does not exist!"));
        mapper.map(request, partnerAsset);
        partnerAsset.setUpdatedBy(userCurrent.getId());
        if (request.getDriverUserId() != null || request.getDriverAssistantUserId() != null) {
            Driver driver = driverRepository.findByUserId(request.getDriverUserId());
            if (driver == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Driver Id");
            }
            Driver driver2 = driverRepository.findByUserId(request.getDriverAssistantUserId());
            if (driver2 == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Driver Assistant Id");
            }
            User user = userRepository.getOne(driver.getUserId());
            User user2 = userRepository.getOne(driver2.getUserId());

            partnerAsset.setDriverId(driver.getId());
            partnerAsset.setDriverUserId(request.getDriverUserId());
            partnerAsset.setDriverAssistantUserId(request.getDriverAssistantUserId());
            partnerAsset.setDriverAssistantId(driver2.getId());
            partnerAsset.setDriverName(user.getLastName() + " " + user.getFirstName());
            partnerAsset.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());
        }
            partnerAssetRepository.save(partnerAsset);
        log.debug("partnerAsset record updated - {}"+ new Gson().toJson(partnerAsset));
        PartnerAssetResponseDto partnerAssetResponseDto = mapper.map(partnerAsset, PartnerAssetResponseDto.class);

        if ((request.getPartnerAssetTypeId() != null || request.getBrandId() != null || request.getColorId() != null )) {
            PartnerAssetType partnerAssetType = partnerAssetTypeRepository.findPartnerAssetTypeById(request.getPartnerAssetTypeId());
            if (partnerAssetType == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid PartnerAssetType Id");
            }
            Partner partner = partnerRepository.findPartnerById(partnerAssetType.getPartnerId());
            if (partner == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid Partner Id");
            }
            AssetTypeProperties assetTypeProperties = assetTypePropertiesRepository.findAssetTypePropertiesById(partnerAssetType.getAssetTypeId());
            if (assetTypeProperties == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid AssetTypeProperties Id");
            }
            Brand brand = brandRepository.findBrandById(request.getBrandId());
            if (brand == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid Brand Id");
            }
            Color color = colorRepository.findColorById(request.getColorId());
            if (color == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Color Id");
            }
            partnerAssetResponseDto.setPartnerName(partner.getName());
            partnerAssetResponseDto.setBrandName(brand.getName());
            partnerAssetResponseDto.setColorName(color.getName());
            partnerAssetResponseDto.setAssetTypeName(assetTypeProperties.getName());

        }
        log.info("Check assset ::::::::::::::::::::::::::::::::::::::::::::::::::::::: " +partnerAsset);
        DriverAsset driverAssetDto = new DriverAsset();
        DriverAssetDto processDriver = new DriverAssetDto();
        DriverAsset saveDrivedAsset = new DriverAsset();

        driverAssetDto = driverAssetRepository.findByDriverIdAndPartnerAssetId(partnerAsset.getDriverId(), partnerAsset.getId());
        if (driverAssetDto == null) {
            processDriver.setPartnerAssetId(partnerAsset.getId());
            processDriver.setDriverType(DriverType.DRIVER);
            processDriver.setDriverId(partnerAsset.getDriverId());
            processDriver.setAssestTypeName(partnerAsset.getAssetTypeName());

            responseDto =  driverAssetService.createDriverAsset(processDriver,request1);
        } else {
            processDriver.setPartnerAssetId(partnerAsset.getId());
            processDriver.setDriverType(DriverType.DRIVER);
            processDriver.setDriverId(partnerAsset.getDriverId());
            processDriver.setAssestTypeName(partnerAsset.getAssetTypeName());
            processDriver.setId(driverAssetDto.getId());
            responseDto =  driverAssetService.updateDriverAsset(processDriver,request1);
        }


        saveDrivedAsset = driverAssetRepository.findByDriverIdAndPartnerAssetId(partnerAsset.getDriverAssistantId(),partnerAsset.getId());
        if (saveDrivedAsset == null){
            processDriver.setPartnerAssetId(partnerAsset.getId());
            processDriver.setDriverType(DriverType.DRIVER_ASSISTANT);
            processDriver.setDriverId(partnerAsset.getDriverAssistantId());
            processDriver.setAssestTypeName(partnerAsset.getAssetTypeName());

            driverAssetService.createDriverAsset(processDriver,request1);
        } else {
            processDriver.setPartnerAssetId(partnerAsset.getId());
            processDriver.setDriverType(DriverType.DRIVER_ASSISTANT);
            processDriver.setDriverId(partnerAsset.getDriverAssistantId());
            processDriver.setAssestTypeName(partnerAsset.getAssetTypeName());
            processDriver.setId(saveDrivedAsset.getId());
            driverAssetService.updateDriverAsset(processDriver,request1);
        }

        return partnerAssetResponseDto;
    }


    public PartnerAssetResponseDto findPartnerAsset(Long id){
        PartnerAsset partnerAsset  = partnerAssetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested PartnerAsset Id does not exist!"));
        PartnerAssetResponseDto partnerAssetResponseDto = mapper.map(partnerAsset, PartnerAssetResponseDto.class);
        partnerAssetResponseDto.setPartnerAssetPictures(getAllPartnerAssetPicture(id));

        PartnerAssetType partnerAssetType = partnerAssetTypeRepository.findPartnerAssetTypeById(partnerAssetResponseDto.getPartnerAssetTypeId());
        if (partnerAssetType == null) {
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " PartnerAssetType does not exist");
        }
        Partner partner = partnerRepository.findPartnerById(partnerAssetType.getPartnerId());
        if (partner == null) {
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Partner does not exist");
        }
        AssetTypeProperties assetTypeProperties = assetTypePropertiesRepository.findAssetTypePropertiesById(partnerAssetType.getAssetTypeId());
        if (assetTypeProperties == null) {
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " AssetTypeProperties does not exist");
        }
        Brand brand = brandRepository.findBrandById(partnerAssetResponseDto.getBrandId());
        if (brand == null) {
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Brand does not exist");
        }
        Color color = colorRepository.findColorById(partnerAssetResponseDto.getColorId());
        if (color == null) {
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Color does not exist");
        }
        Driver driver = driverRepository.findDriverById(partnerAssetResponseDto.getDriverId());
        if (driver == null) {
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Driver does not exist");
        }
        Driver driver2 = driverRepository.findDriverById(partnerAssetResponseDto.getDriverAssistantId());
        if (driver2 == null) {
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Driver Assistant does not exist");
        }
        User user = userRepository.getOne(driver.getUserId());
        User user2 = userRepository.getOne(driver2.getUserId());

        partnerAssetResponseDto.setPartnerName(partner.getName());
        partnerAssetResponseDto.setBrandName(brand.getName());
        partnerAssetResponseDto.setColorName(color.getName());
        partnerAssetResponseDto.setDriverUserId(driver.getUserId());
        partnerAssetResponseDto.setDriverAssistantUserId(driver2.getUserId());
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
            PartnerAssetType partnerAssetType = partnerAssetTypeRepository.findPartnerAssetTypeById(partnerAsset.getPartnerAssetTypeId());
            if (partnerAssetType == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid PartnerAssetType Id");
            }
            Partner partner = partnerRepository.findPartnerById(partnerAssetType.getPartnerId());
            if (partner == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid Partner Id");
            }
            AssetTypeProperties assetTypeProperties = assetTypePropertiesRepository.findAssetTypePropertiesById(partnerAssetType.getAssetTypeId());
            if (assetTypeProperties == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid AssetTypeProperties Id");
            }
            asset.setPartnerName(partner.getName());
            asset.setAssetTypeName(assetTypeProperties.getName());

            Brand brand = brandRepository.findBrandById(partnerAsset.getBrandId());
            if (brand == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid Brand Id");
            }
            asset.setBrandName(brand.getName());

            Color color = colorRepository.findColorById(partnerAsset.getColorId());
            if (color == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Color Id");
            }
            asset.setColorName(color.getName());

            Driver driver = driverRepository.findDriverById(partnerAsset.getDriverId());
            if (driver == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Driver Id");
            }
            User user = userRepository.getOne(driver.getUserId());
            asset.setDriverName(user.getLastName() + " " + user.getFirstName());

            Driver driver2 = driverRepository.findDriverById(partnerAsset.getDriverAssistantId());
            if (driver2 == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Driver Assistant Id");
            }
            User user2 = userRepository.getOne(driver2.getUserId());
            asset.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());

            asset.setDriverUserId(driver.getUserId());
            asset.setDriverAssistantUserId(driver2.getUserId());

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

            PartnerAssetType partnerAssetType = partnerAssetTypeRepository.findPartnerAssetTypeById(asset.getPartnerAssetTypeId());
            if (partnerAssetType == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid PartnerAssetType Id");
            }
            Partner partner = partnerRepository.findPartnerById(partnerAssetType.getPartnerId());
            if (partner == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid Partner Id");
            }
            AssetTypeProperties assetTypeProperties = assetTypePropertiesRepository.findAssetTypePropertiesById(partnerAssetType.getAssetTypeId());
            if (assetTypeProperties == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid AssetTypeProperties Id");
            }

            Brand brand = brandRepository.findBrandById(asset.getBrandId());
            if (brand == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION , " Invalid Brand Id");
            }
            Color color = colorRepository.findColorById(asset.getColorId());
            if (color == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Color Id");
            }
            Driver driver = driverRepository.findDriverById(asset.getDriverId());
            if (driver == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Driver Id");
            }
            User user = userRepository.getOne(driver.getUserId());
            Driver driver2 = driverRepository.findDriverById(asset.getDriverAssistantId());
            if (driver2 == null) {
                throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " Invalid Driver Assistant Id");
            }
            User user2 = userRepository.getOne(driver2.getUserId());
            asset.setDriverAssistantName(user2.getLastName() + " " + user2.getFirstName());


            asset.setPartnerName(partner.getName());
            asset.setBrandName(brand.getName());
            asset.setColorName(color.getName());
            asset.setDriverName(user.getLastName() + " " + user.getFirstName());
            asset.setAssetTypeName(assetTypeProperties.getName());
            asset.setDriverUserId(driver.getUserId());
            asset.setDriverAssistantUserId(driver2.getUserId());

        }

        return partnerAssets;

    }

    public List<PartnerAssetPicture> getAllPartnerAssetPicture(Long partnerAssetId){
        List<PartnerAssetPicture> partnerAssetPictures = partnerAssetPictureRepository.findByPartnerAssetId(partnerAssetId);
        return partnerAssetPictures;

    }
}
