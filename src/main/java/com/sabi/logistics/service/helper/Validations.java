package com.sabi.logistics.service.helper;



import com.sabi.framework.exceptions.BadRequestException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.*;
import com.sabi.logistics.core.models.*;
import com.sabi.logistics.service.repositories.CategoryRepository;
import com.sabi.logistics.service.repositories.LGARepository;
import com.sabi.logistics.service.repositories.PartnerPropertiesRepository;
import com.sabi.logistics.service.repositories.StateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@SuppressWarnings("All")
@Slf4j
@Service
public class Validations {



    private StateRepository stateRepository;
    private LGARepository lgaRepository;
    private UserRepository userRepository;
    private PartnerPropertiesRepository partnerPropertiesRepository;
    private CategoryRepository categoryRepository;


    public Validations(StateRepository stateRepository, LGARepository lgaRepository, UserRepository userRepository, PartnerPropertiesRepository partnerPropertiesRepository, CategoryRepository categoryRepository) {
        this.stateRepository = stateRepository;
        this.lgaRepository = lgaRepository;
        this.userRepository = userRepository;
        this.partnerPropertiesRepository = partnerPropertiesRepository;
        this.categoryRepository = categoryRepository;
    }

    public void validateState(StateDto stateDto) {
        if (stateDto.getName() == null || stateDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
    }


    public void validateLGA (LGADto lgaDto){
        if (lgaDto.getName() == null || lgaDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");

        State state = stateRepository.findById(lgaDto.getStateId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid State id!"));
    }


    public void validateCountry(CountryDto countryDto) {
        if (countryDto.getName() == null || countryDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        if(countryDto.getCode() == null || countryDto.getCode().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Code cannot be empty");
    }


    public void validateCategory(CategoryDto categoryDto) {
        if (categoryDto.getName() == null || categoryDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");

    }

    public void validateAssetTypeProperties(AssetTypePropertiesDto assetTypePropertiesDto) {
        if (assetTypePropertiesDto.getName() == null || assetTypePropertiesDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        if (assetTypePropertiesDto.getDescription() == null || assetTypePropertiesDto.getDescription().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Description cannot be empty");

    }

    public void validatePartnerProperties(PartnerPropertiesDto partnerPropertiesDto) {
        if (partnerPropertiesDto.getName() == null || partnerPropertiesDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        if (partnerPropertiesDto.getAddress() == null || partnerPropertiesDto.getAddress().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Address cannot be empty");
        LGA lga = lgaRepository.findById(partnerPropertiesDto.getLgaId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid LGA id!"));
        if (partnerPropertiesDto.getPhone() == null || partnerPropertiesDto.getPhone().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Phone cannot be empty");
        if (partnerPropertiesDto.getEmail() == null || partnerPropertiesDto.getEmail().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Email cannot be empty");
        if (partnerPropertiesDto.getWebSite() == null || partnerPropertiesDto.getWebSite().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Website cannot be empty");
//        if (partnerPropertiesDto.getEmployeeCount() < 0 )
//            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Employee count cannot be empty");

    }

    public void validateBlockType(BlockTypeDto blockTypeDto) {
        if (blockTypeDto.getName() == null || blockTypeDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        if (blockTypeDto.getLength() <= 0.0)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Length cannot be empty");
        if (blockTypeDto.getHeight() <= 0.0)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Heigth cannot be empty");
        if (blockTypeDto.getWidth() <= 0.0)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Width cannot be empty");
        if(blockTypeDto.getPrice() <= 0.0)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "price cannot be empty");
    }

    public void validateClient(ClientDto clientDto) {
        User user = userRepository.findById(clientDto.getUserId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid user id!"));
    }

    public void validatePartnerCategories(PartnerCategoriesDto partnerCategoriesDto) {
        PartnerProperties partnerProperties = partnerPropertiesRepository.findById(partnerCategoriesDto.getPartnerId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid partner id!"));
        Category category = categoryRepository.findById(partnerCategoriesDto.getCategoryId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid category id!"));
    }

    public void validatePartnerLocation(PartnerLocationDto partnerLocationDto) {
        PartnerProperties partnerProperties = partnerPropertiesRepository.findById(partnerLocationDto.getPartnerId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid partner id!"));
        State state = stateRepository.findById(partnerLocationDto.getStateId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid state id!"));
        if(partnerLocationDto.getWareHouses() < 0)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Enter a valid ware house figure!");
    }

    public void validateDriver(DriverDto driverDto) {

        if (driverDto.getName() == null || driverDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        if(driverDto.getPartnerAssetId()==null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Partner asset id cannot be empty");
        if(driverDto.getUserId()==null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "User id cannot be empty");
    }


    public void validateDriverAsset(DriverAssetDto driverAssetDto) {

        if (driverAssetDto.getName() == null || driverAssetDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        if(driverAssetDto.getPartnerAssetId()==null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Partner asset id cannot be empty");
        if(driverAssetDto.getDriverId()==null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Driver id cannot be empty");
    }


    public void validatePartnerPicture(PartnerAssetPictureDto partnerAssetPictureDto) {


        if(partnerAssetPictureDto.getPartnerAssetId()==null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Partner asset id cannot be empty");

    }
}


