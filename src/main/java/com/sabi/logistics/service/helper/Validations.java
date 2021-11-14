package com.sabi.logistics.service.helper;


import com.sabi.framework.exceptions.BadRequestException;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.framework.utils.Utility;
import com.sabi.logistics.core.dto.request.*;
import com.sabi.logistics.core.models.*;
import com.sabi.logistics.service.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@SuppressWarnings("All")
@Slf4j
@Service
public class Validations {



    private CountryRepository countryRepository;
    private StateRepository stateRepository;
    private LGARepository lgaRepository;
    private UserRepository userRepository;
    private PartnerRepository partnerRepository;
    private CategoryRepository categoryRepository;
    private final AssetTypePropertiesRepository assetTypePropertiesRepository;
    private final PartnerAssetRepository partnerAssetRepository;
    private final PartnerAssetTypeRepository partnerAssetTypeRepository;
    private final DriverRepository driverRepository;
    private final BrandRepository brandRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;




    public Validations(CountryRepository countryRepository,StateRepository stateRepository, LGARepository lgaRepository, UserRepository userRepository,
                       PartnerRepository partnerRepository, CategoryRepository categoryRepository,
                       AssetTypePropertiesRepository assetTypePropertiesRepository, PartnerAssetRepository partnerAssetRepository,
                       PartnerAssetTypeRepository partnerAssetTypeRepository, DriverRepository driverRepository,
                       BrandRepository brandRepository) {
        this.countryRepository = countryRepository;
        this.stateRepository = stateRepository;
        this.lgaRepository = lgaRepository;
        this.userRepository = userRepository;
        this.partnerRepository = partnerRepository;
        this.categoryRepository = categoryRepository;
        this.assetTypePropertiesRepository = assetTypePropertiesRepository;
        this.partnerAssetRepository = partnerAssetRepository;
        this.partnerAssetTypeRepository = partnerAssetTypeRepository;
        this.driverRepository = driverRepository;
        this.brandRepository = brandRepository;
    }

    public void validateState(StateDto stateDto) {
        if (stateDto.getName() == null || stateDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        Country country = countryRepository.findById(stateDto.getCountryId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid Country id!"));
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

    public void validatePartnerProperties(PartnerDto partnerPropertiesDto) {
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
        Partner partnerProperties = partnerRepository.findById(partnerCategoriesDto.getPartnerId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid partner id!"));
        Category category = categoryRepository.findById(partnerCategoriesDto.getCategoryId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid category id!"));
    }

    public void validatePartnerLocation(PartnerLocationDto partnerLocationDto) {
        Partner partnerProperties = partnerRepository.findById(partnerLocationDto.getPartnerId())
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

//        if(partnerAssetPictureDto.getPartnerAssetId()==null)
//            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Partner asset id cannot be empty");
//        PartnerAsset partnerAsset = partnerAssetRepository.getOne(partnerAssetPictureDto.getPartnerAssetId());
//        if (partnerAsset == null){
//
//        }

        PartnerAsset partnerAsset = partnerAssetRepository.findById(partnerAssetPictureDto.getPartnerAssetId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid partner asset id!"));
    }




    public void validatePartner(PartnerSignUpDto partner){
        if (partner.getFirstName() == null || partner.getFirstName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "First name cannot be empty");
        if (partner.getFirstName().length() < 2 || partner.getFirstName().length() > 100)// NAME LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid first name  length");

        if (partner.getLastName() == null || partner.getLastName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Last name cannot be empty");
        if (partner.getLastName().length() < 2 || partner.getLastName().length() > 100)// NAME LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid last name  length");

        if (partner.getEmail() == null || partner.getEmail().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "email cannot be empty");
        if (!Utility.validEmail(partner.getEmail().trim()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid Email Address");
        User user = userRepository.findByEmail(partner.getEmail());
        if(user !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Email already exist");
        }
        if (partner.getPhone() == null || partner.getPhone().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Phone number cannot be empty");
        if (partner.getPhone().length() < 8 || partner.getPhone().length() > 14)// NAME LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid phone number  length");
        if (!Utility.isNumeric(partner.getPhone()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for phone number ");
        if (partner.getName() == null || partner.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
    }






    public void validatePartnerUser(PartnerUserRequestDto request){
        if (request.getFirstName() == null || request.getFirstName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "First name cannot be empty");
        if (request.getFirstName().length() < 2 || request.getFirstName().length() > 100)// NAME LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid first name  length");

        if (request.getLastName() == null || request.getLastName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Last name cannot be empty");
        if (request.getLastName().length() < 2 || request.getLastName().length() > 100)// NAME LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid last name  length");

        if (request.getEmail() == null || request.getEmail().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "email cannot be empty");
        if (!Utility.validEmail(request.getEmail().trim()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid Email Address");
        User user = userRepository.findByEmail(request.getEmail());
        if(user !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Email already exist");
        }
        if (request.getPhone() == null || request.getPhone().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Phone number cannot be empty");
        if (request.getPhone().length() < 8 || request.getPhone().length() > 14)// NAME LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid phone number  length");
        if (!Utility.isNumeric(request.getPhone()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for phone number ");
        User userExist = userRepository.findByPhone(request.getPhone());
        if(userExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "  user phone already exist");
        }
    }


    public void validateBrand(BrandRequestDto request) {
        if(request.getName() != null && !request.getName().isEmpty()){}
        else throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Brand Name cannot be empty");
    }


    public void validateWarehouse(WarehouseRequestDto request) {
//        if(request.getPartnerId().)
        lgaRepository.findById(request.getLgaId()).orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                " Enter a valid lga id!"));
        //todo check for existing partner id
        partnerRepository.findById(request.getPartnerId()).orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                " Enter a valid partner id!"));
    }

    public void validatePartnerAssetType(PartnerAssetTypeRequestDto request) {
        assetTypePropertiesRepository.findById(request.getAssetTypeId()).orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                " Enter a valid asset type id!"));
        partnerRepository.findById(request.getPartnerId()).orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                " Enter a valid partner id!"));
    }

    public void validatePartnerAsset(PartnerAssetRequestDto request) {
        partnerAssetTypeRepository.findById(request.getPartnerAssetTypeId()).orElseThrow(()-> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                " Enter a valid Partner Asset Type!"));
        driverRepository.findById(request.getDriverId()).orElseThrow(()-> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                " Enter a valid Driver!"));
        brandRepository.findById(request.getBrandId()).orElseThrow(()-> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                " Enter a valid Brand!"));
    }



    public String generateReferenceNumber(int numOfDigits) {
        if (numOfDigits < 1) {
            throw new IllegalArgumentException(numOfDigits + ": Number must be equal or greater than 1");
        }
        long random = (long) Math.floor(Math.random() * 9 * (long) Math.pow(10, numOfDigits - 1)) + (long) Math.pow(10, numOfDigits - 1);
        return Long.toString(random);
    }

    public void validateOrder (OrderRequestDto request){

        if(request.getWareHouseID() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, " wareHouseID can not be null");
        if (!Utility.isNumeric(request.getWareHouseID().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for wareHouseID ");

        if (request.getDeliveryPartnerID() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Delivery Partner ID cannot be empty");
        if (!Utility.isNumeric(request.getDeliveryPartnerID().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for Delivery Partner ID ");

        if (request.getDeliveryStatus() == null || request.getDeliveryStatus().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Delivery Status cannot be empty");
        if (!("Pending".equalsIgnoreCase(request.getDeliveryStatus()) || "Ongoing".equalsIgnoreCase(request.getDeliveryStatus()) || "Completed".equalsIgnoreCase(request.getDeliveryStatus())))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Enter the correct Delivery Status");
        if (!Utility.validateName(request.getDeliveryStatus().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for Delivery Status ");

        if (request.getCustomerName() == null || request.getCustomerName().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Customer Name cannot be empty");
        if (!Utility.validateName(request.getCustomerName().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for Customer Name ");

        if (request.getCustomerPhone() == null || request.getCustomerPhone().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Customer Phone cannot be empty");
        if (!Utility.validatePhoneNumber(request.getCustomerPhone().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for Customer Phone ");

        if (request.getDeliveryAddress() == null || request.getDeliveryAddress().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Delivery Address cannot be empty");

        warehouseRepository.findById(request.getWareHouseID()).orElseThrow(() ->
                new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Warehouse ID does not Exist!")
        );
    }

    public void validateOrderItem (OrderItemRequestDto request){

        if (request.getDeliveryStatus() == null || request.getDeliveryStatus().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Delivery Status cannot be empty");
        if (!("Pending".equalsIgnoreCase(request.getDeliveryStatus()) || "InTransit".equalsIgnoreCase(request.getDeliveryStatus()) || "Completed".equalsIgnoreCase(request.getDeliveryStatus())))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Enter the correct Delivery Status");
        if (!Utility.validateName(request.getDeliveryStatus().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for Delivery Status ");

        if (request.getPartnerAssetID() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Partner Asset ID cannot be empty");
        if (!Utility.isNumeric(request.getPartnerAssetID().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for Partner Asset ID ");


        if (request.getName() == null || request.getName().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        if (!Utility.validateName(request.getName().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for Name ");

        if (request.getQty() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Qty cannot be empty");
        if (!Utility.isNumeric(request.getQty().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for Qty");


        partnerAssetRepository.findById(request.getPartnerAssetID()).orElseThrow(() ->
                new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " PartnerAssetID ID does not Exist!")
        );
    }
}


