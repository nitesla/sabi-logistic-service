package com.sabi.logistics.service.helper;


import com.sabi.framework.exceptions.BadRequestException;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.Role;
import com.sabi.framework.models.User;
import com.sabi.framework.repositories.RoleRepository;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.framework.utils.Utility;
import com.sabi.logistics.core.dto.request.*;
import com.sabi.logistics.core.enums.TransAction;
import com.sabi.logistics.core.models.*;
import com.sabi.logistics.service.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;

@SuppressWarnings("All")
@Slf4j
@Service
public class Validations {


    private RoleRepository roleRepository;
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

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private TripRequestRepository tripRequestRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private BankRepository bankRepository;

    @Autowired
    private DriverAssetRepository driverAssetRepository;

    @Autowired
    private DropOffRepository dropOffRepository;

    @Autowired
    private  DriverWalletRepository driverWalletRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private PricingConfigurationRepository pricingConfigurationRepository;

    @Autowired
    private DashboardSummaryRepository dashboardSummaryRepository;

    @Autowired
    private  RouteLocationRepository routeLocationRepository;




    public Validations(RoleRepository roleRepository,CountryRepository countryRepository,StateRepository stateRepository, LGARepository lgaRepository, UserRepository userRepository,
                       PartnerRepository partnerRepository, CategoryRepository categoryRepository,
                       AssetTypePropertiesRepository assetTypePropertiesRepository, PartnerAssetRepository partnerAssetRepository,
                       PartnerAssetTypeRepository partnerAssetTypeRepository, DriverRepository driverRepository,
                       BrandRepository brandRepository) {
        this.roleRepository = roleRepository;
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
        String valName = stateDto.getName();
        char valCharName = valName.charAt(0);
        if (Character.isDigit(valCharName)){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name can not start with a number");
        }
        Country country = countryRepository.findById(stateDto.getCountryId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid Country id!"));
    }


    public void validateLGA (LGADto lgaDto){
        if (lgaDto.getName() == null || lgaDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");

        String valName = lgaDto.getName();
        char valCharName = valName.charAt(0);
        if (Character.isDigit(valCharName)){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name can not start with a number");
        }

        State state = stateRepository.findById(lgaDto.getStateId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid State id!"));
    }

    public void validatePartnerUserActivation (PartnerUserActivation request){
        if (request.getEmail() == null || request.getEmail().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Email cannot be empty");
        if (!Utility.validEmail(request.getEmail().trim()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid Email Address");
        if(request.getActivationUrl()== null || request.getActivationUrl().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Activation url cannot be empty");


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
        String valName = categoryDto.getName();
        char valCharName = valName.charAt(0);
        if (Character.isDigit(valCharName)){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name can not start with a number");
        }
    }

    public void validateAssetTypeProperties(AssetTypePropertiesDto assetTypePropertiesDto) {
        if (assetTypePropertiesDto.getName() == null || assetTypePropertiesDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        String valName = assetTypePropertiesDto.getName();
        char valCharName = valName.charAt(0);
        if (Character.isDigit(valCharName)){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name can not start with a number");
        }
        if (assetTypePropertiesDto.getDescription() == null || assetTypePropertiesDto.getDescription().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Description cannot be empty");
        String valDescription = assetTypePropertiesDto.getDescription();
        char valCharDescription = valDescription.charAt(0);
        if (Character.isDigit(valCharDescription)){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Description can not start with a number");
        }
    }

    public void validatePartnerProperties(CompleteSignupRequest partnerPropertiesDto) {
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
    }



    public void validatePartnerUpdate(PartnerDto partnerPropertiesDto) {
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
    }


    public void validateBlockType(BlockTypeDto blockTypeDto) {
        if (blockTypeDto.getName() == null || blockTypeDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        String valName = blockTypeDto.getName();
        char valCharName = valName.charAt(0);
        if (Character.isDigit(valCharName)){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name can not start with a number");
        }
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




    public void validateDriverAsset(DriverAssetDto driverAssetDto) {

//        if (!driverAssetDto.getName().isEmpty() ){
//            String valName = driverAssetDto.getName();
//            char valCharName = valName.charAt(0);
//            if (Character.isDigit(valCharName)){
//                throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name can not start with a number");
//            }
//        }

        if(driverAssetDto.getPartnerAssetId()==null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Partner asset id cannot be empty");
        if(driverAssetDto.getDriverId()==null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Driver id cannot be empty");

    }
    
    public void validatePartnerPicture(PartnerAssetPictureDto partnerAssetPictureDto) {

        PartnerAsset partnerAsset = partnerAssetRepository.findById(partnerAssetPictureDto.getPartnerAssetId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid partner asset id!"));
        if (partnerAssetPictureDto.getImage() == null || partnerAssetPictureDto.getImage().isEmpty()){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST,
                    " Image can not be empty!");
        }
        if (!("Front".equalsIgnoreCase(partnerAssetPictureDto.getPictureType()) || "Side".equalsIgnoreCase(partnerAssetPictureDto.getPictureType()) || "Haulage".equalsIgnoreCase(partnerAssetPictureDto.getPictureType()))) {
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Enter a correct picture type");
        }
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

        if(request.getUserType() == null || request.getUserType().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "User type cannot be empty");

        if (request.getUserType() != null || !request.getUserType().isEmpty()) {

            if (!PartnerConstants.DRIVER_USER.equals(request.getUserType())
                    && !PartnerConstants.PARTNER_USER.equals(request.getUserType()))
                throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid User category type");
        }

        if(request.getRoleId() == null){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Role id cannot be empty");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid role id!"));

    }


    public void validateBrand(BrandRequestDto request) {
        if(request.getName() != null && !request.getName().isEmpty()){}
        else throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Brand Name cannot be empty");
    }


    public void validateWarehouse(WarehouseRequestDto request) {
        lgaRepository.findById(request.getLgaId()).orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                " Enter a valid lga id!"));
        //todo check for existing partner id
        partnerRepository.findById(request.getPartnerId()).orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                " Enter a valid partner id!"));
        userRepository
                .findById(request.getUserId()).orElseThrow(()-> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Enter a valid user Id"));
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
        if (request.getDriverUserId() != null && request.getDriverAssistantUserId() != null) {
            if (request.getDriverUserId().equals(request.getDriverAssistantUserId())) {
                throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " driverUser Id and driverAssistantUser Id can not be same!");
            }
        }
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

    public String generateCode(String code) {
        String encodedString = Base64.getEncoder().encodeToString(code.getBytes());
        return encodedString;
    }

    public void validateOrder (OrderRequestDto request){

        if (request.getDeliveryStatus() == null || request.getDeliveryStatus().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Delivery Status cannot be empty");
        if (!("Pending".equalsIgnoreCase(request.getDeliveryStatus()) || "Ongoing".equalsIgnoreCase(request.getDeliveryStatus()) || "Completed".equalsIgnoreCase(request.getDeliveryStatus()) ||"Cancelled".equalsIgnoreCase(request.getDeliveryStatus())))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Enter the correct Delivery Status");

        if (request.getCustomerName() == null || request.getCustomerName().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Customer Name cannot be empty");

        if (request.getCustomerPhone() == null || request.getCustomerPhone().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Customer Phone cannot be empty");
        if (!Utility.validatePhoneNumber(request.getCustomerPhone().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for Customer Phone ");

        if (request.getDeliveryAddress() == null || request.getDeliveryAddress().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Delivery Address cannot be empty");

        if (request.getTotalAmount() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Total Amount cannot be empty");
        if (request.getTotalAmount()  <= 0.0)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Total Amount cannot be less than 0");
        if (!Utility.isNumeric(request.getTotalAmount().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for Total Amount");

        if (request.getTotalQuantity() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Qty cannot be empty");
        if (!Utility.isNumeric(request.getTotalQuantity().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for Qty");

    }

    public void validateOrderOrderItems (OrderOrderItemDto request){

        if (request.getDeliveryStatus() == null || request.getDeliveryStatus().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Delivery Status cannot be empty");
        if (!("Pending".equalsIgnoreCase(request.getDeliveryStatus()) || "Ongoing".equalsIgnoreCase(request.getDeliveryStatus()) || "Completed".equalsIgnoreCase(request.getDeliveryStatus()) ||"Cancelled".equalsIgnoreCase(request.getDeliveryStatus())))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Enter the correct Delivery Status");

        if (request.getCustomerName() == null || request.getCustomerName().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Customer Name cannot be empty");

        if (request.getCustomerPhone() == null || request.getCustomerPhone().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Customer Phone cannot be empty");
        if (!Utility.validatePhoneNumber(request.getCustomerPhone().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for Customer Phone ");

        if (request.getDeliveryAddress() == null || request.getDeliveryAddress().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Delivery Address cannot be empty");

        if (request.getTotalAmount() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Total Amount cannot be empty");
        if (request.getTotalAmount()  <= 0.0)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Total Amount cannot be less than 0");
        if (!Utility.isNumeric(request.getTotalAmount().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for Total Amount");

        if (request.getTotalQuantity() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Qty cannot be empty");
        if (!Utility.isNumeric(request.getTotalQuantity().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for Qty");



    }

    public void validateOrderItem (OrderItemRequestDto request){

        if(request.getWareHouseId() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, " wareHouseId can not be null");
        if (!Utility.isNumeric(request.getWareHouseId().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for wareHouseId ");

        if (request.getDeliveryStatus() == null || request.getDeliveryStatus().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Delivery Status cannot be empty");
        if (!("pending".equalsIgnoreCase(request.getDeliveryStatus())  || "AwaitingDelivery".equalsIgnoreCase(request.getDeliveryStatus())  || "InTransit".equalsIgnoreCase(request.getDeliveryStatus()) || "Returned".equalsIgnoreCase(request.getDeliveryStatus()) || "Completed".equalsIgnoreCase(request.getDeliveryStatus())))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Enter the correct Delivery Status");

//        if (request.getOrderId() == null )
//            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "orderId cannot be empty");
        if (!Utility.isNumeric(request.getOrderId().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for orderId ");


        if (request.getProductName() == null || request.getProductName().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");

        if (request.getQty() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Qty cannot be empty");
        if (!Utility.isNumeric(request.getQty().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for Qty");


        orderRepository.findById(request.getOrderId()).orElseThrow(() ->
                new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " orderId does not Exist!")
        );

        warehouseRepository.findById(request.getWareHouseId()).orElseThrow(() ->
                new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Warehouse Id does not Exist!")
        );
    }

    public void validateTripRequestResponse (TripRequestResponseReqDto request){

        if (request.getTripRequestId() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "tripRequestId cannot be empty");
        if (!Utility.isNumeric(request.getTripRequestId().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for tripRequestId ");

        if (request.getPartnerId() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "partnerId cannot be empty");
        if (!Utility.isNumeric(request.getPartnerId().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for partnerId");

        if (request.getStatus() == null || request.getStatus().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Status cannot be empty");
        if (!("Pending".equalsIgnoreCase(request.getStatus()) || "Rejected".equalsIgnoreCase(request.getStatus()) || "Accepted".equalsIgnoreCase(request.getStatus()) || "Cancelled".equalsIgnoreCase(request.getStatus())))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Enter the correct Status");


        tripRequestRepository.findById(request.getTripRequestId()).orElseThrow(() ->
                new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " tripRequestId does not Exist!")
        );

        partnerRepository.findById(request.getPartnerId()).orElseThrow(() ->
                new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " partnerId does not Exist!")
        );
    }

    public void validateDropOffItem (DropOffItemRequestDto request){

        if (request.getDropOffId() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "dropOffId cannot be empty");
        if (!Utility.isNumeric(request.getDropOffId().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for dropOffId ");

        if (request.getOrderItemId() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "orderItemId cannot be empty");
        if (!Utility.isNumeric(request.getOrderItemId().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for orderItemId");

        if (request.getStatus() == null || request.getStatus().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Status cannot be empty");
        if (!("pending".equalsIgnoreCase(request.getStatus())  || "DriverArrived".equalsIgnoreCase(request.getStatus()) || "cancelled".equalsIgnoreCase(request.getStatus()) || "InTransit".equalsIgnoreCase(request.getStatus()) || "failed".equalsIgnoreCase(request.getStatus()) || "returned".equalsIgnoreCase(request.getStatus()) || "completed".equalsIgnoreCase(request.getStatus())))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Enter the correct Status for dropOffItem");


        orderItemRepository.findById(request.getOrderItemId()).orElseThrow(() ->
                new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " orderItemId does not Exist!")
        );
        dropOffRepository.findById(request.getDropOffId()).orElseThrow(() ->
                new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " dropOffId does not Exist!")
        );
    }

    public void validateDropOff (DropOffRequestDto request){

        if (request.getTripRequestId() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "tripRequestId cannot be empty");
        if (!Utility.isNumeric(request.getTripRequestId().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for tripRequestId ");


        if (request.getPhoneNo() == null || request.getPhoneNo().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Phone number cannot be empty");
        if (request.getPhoneNo().length() < 8 || request.getPhoneNo().length() > 14)// NAME LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid phone number  length");
        if (!Utility.isNumeric(request.getPhoneNo()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for phone number ");

        if (request.getEmail() == null || request.getEmail().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "email cannot be empty");
        if (!Utility.validEmail(request.getEmail().trim()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid Email Address");

        if (request.getOrderId() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "orderId cannot be empty");
        if (!Utility.isNumeric(request.getOrderId().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for orderId ");


        tripRequestRepository.findById(request.getTripRequestId()).orElseThrow(() ->
                new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " tripRequestId does not Exist!")
        );

        orderRepository.findById(request.getOrderId()).orElseThrow(() ->
                new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " orderId does not Exist!")
        );
    }

    public void validateDropOffs (DropOffMasterRequestDto request){

        if (request.getTripRequestId() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "tripRequestId cannot be empty");
        if (!Utility.isNumeric(request.getTripRequestId().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for tripRequestId ");


        if (request.getPhoneNo() == null || request.getPhoneNo().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Phone number cannot be empty");
        if (request.getPhoneNo().length() < 8 || request.getPhoneNo().length() > 14)// NAME LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid phone number  length");
        if (!Utility.isNumeric(request.getPhoneNo()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for phone number ");

        if (request.getEmail() == null || request.getEmail().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "email cannot be empty");
        if (!Utility.validEmail(request.getEmail().trim()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid Email Address");

        if (request.getOrderId() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "orderId cannot be empty");
        if (!Utility.isNumeric(request.getOrderId().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for orderId ");


        tripRequestRepository.findById(request.getTripRequestId()).orElseThrow(() ->
                new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " tripRequestId does not Exist!")
        );

        orderRepository.findById(request.getOrderId()).orElseThrow(() ->
                new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " orderId does not Exist!")
        );
    }

    public void validateProduct (ProductRequestDto request){

        if (request.getThirdPartyId() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "thirdPartyId cannot be empty");
        if (!Utility.isNumeric(request.getThirdPartyId().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for thirdPartyId ");

        if (request.getTotalStock() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "totalStock cannot be empty");
        if (!Utility.isNumeric(request.getTotalStock().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for totalStock");

        if (request.getStockSold() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "stockSold cannot be empty");
        if (!Utility.isNumeric(request.getStockSold().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for stockSold");


        if (request.getName() == null || request.getName().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");


    }

    public void validateTripRequest (TripRequestDto request){

        if (request.getPartnerId() != null && !Utility.isNumeric(request.getPartnerId().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for partnerId ");
        if (request.getPartnerAssetId() != null && !Utility.isNumeric(request.getPartnerAssetId().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for partnerAssetId ");

        if (request.getStatus() == null || request.getStatus().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Status cannot be empty");

        if (request.getDeliveryStatus() == null || request.getDeliveryStatus().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Delivery Status cannot be empty");

        if (request.getWareHouseId() == null && (request.getContactPerson() == null || request.getContactPerson().isEmpty())) {
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Contact Person cannot be empty");
        } else if(request.getWareHouseId() == null && (request.getContactEmail() == null || request.getContactEmail().isEmpty())){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Contact Email cannot be empty");
        } else if(request.getWareHouseId() == null && (request.getContactPhone() == null || request.getContactPhone().isEmpty())){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Contact Phone cannot be empty");
        } else if(request.getWareHouseId() == null && (request.getWareHouseAddress() == null || request.getWareHouseAddress().isEmpty())) {
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "WareHouse Address cannot be empty");
        }

        if (request.getWareHouseId() != null) {
            warehouseRepository.findById(request.getWareHouseId()).orElseThrow(() ->
                    new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                            " wareHouseId does not Exist!")
            );
        }

        if (request.getPartnerId() != null) {
            partnerRepository.findById(request.getPartnerId()).orElseThrow(() ->
                    new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                            " partnerId does not Exist!")
            );
        }
    }

    public void validateMasterTripRequest (TripMasterRequestDto request){

        if (request.getPartnerId() != null && !Utility.isNumeric(request.getPartnerId().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for partnerId ");
        if (request.getPartnerAssetId() != null && !Utility.isNumeric(request.getPartnerAssetId().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for partnerAssetId ");

        if (request.getStatus() == null || request.getStatus().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Status cannot be empty");

        if (request.getDeliveryStatus() == null || request.getDeliveryStatus().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Delivery Status cannot be empty");

        if (request.getWareHouseId() == null && (request.getContactPerson() == null || request.getContactPerson().isEmpty())) {
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Contact Person cannot be empty");
        } else if(request.getWareHouseId() == null && (request.getContactEmail() == null || request.getContactEmail().isEmpty())){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Contact Email cannot be empty");
        } else if(request.getWareHouseId() == null && (request.getContactPhone() == null || request.getContactPhone().isEmpty())){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Contact Phone cannot be empty");
        } else if(request.getWareHouseId() == null && (request.getWareHouseAddress() == null || request.getWareHouseAddress().isEmpty())) {
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "WareHouse Address cannot be empty");
        }

        if (request.getWareHouseId() != null) {
            warehouseRepository.findById(request.getWareHouseId()).orElseThrow(() ->
                    new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                            " wareHouseId does not Exist!")
            );
        }

        if (request.getPartnerId() != null) {
            partnerRepository.findById(request.getPartnerId()).orElseThrow(() ->
                    new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                            " partnerId does not Exist!")
            );
        }
    }

    public void validateTripItem (TripItemRequestDto request){

        if (request.getTripRequestId() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "tripRequestId cannot be empty");
        if (!Utility.isNumeric(request.getTripRequestId().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for tripRequestId ");

        if (request.getThirdPartyProductId() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "thirdPartyProductId cannot be empty");
        if (!Utility.isNumeric(request.getThirdPartyProductId().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for thirdPartyProductId ");


        tripRequestRepository.findById(request.getTripRequestId()).orElseThrow(() ->
                new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " tripRequestId does not Exist!")
        );
    }

    public void validateBank(BankDto bankDto) {
        String valName = bankDto.getName();
        char valCharName = valName.charAt(0);
        if (Character.isDigit(valCharName)){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name can not start with a number");
        }
        if (bankDto.getName() == null || bankDto.getName().trim().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        if (bankDto.getCode() == null || bankDto.getCode().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Bank code cannot be empty");
    }

    public void validatePartnerBank (PartnerBankDto request){

        if (request.getPartnerId() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "partnerId cannot be empty");
        if (!Utility.isNumeric(request.getPartnerId().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for partnerId ");

        if (request.getBankId() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "bankId cannot be empty");
        if (!Utility.isNumeric(request.getBankId().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for bankId");


        if (request.getAccountNumber() == null || request.getAccountNumber().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "accountNumber cannot be empty");
        if (!Utility.isNumeric(request.getAccountNumber().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for accountNumber");

        Partner partner = partnerRepository.findById(request.getPartnerId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid partnerId!"));

        Bank bank = bankRepository.findById(request.getBankId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid bankId!"));


    }

    public void validatePaymentTerms (PaymentTermsDto request){

        if (request.getPartnerAssetTypeId() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "partnerAssetTypeId cannot be empty");
        if (!Utility.isNumeric(request.getPartnerAssetTypeId().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for partnerAssetTypeId ");

        if (request.getDays() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "days cannot be empty");
        if (!Utility.isNumeric(request.getDays().toString()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for days");

        PartnerAssetType partnerAssetType = partnerAssetTypeRepository.findById(request.getPartnerAssetTypeId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid partnerAssetTypeId!"));


    }

    public void validateInventory(InventoryDto request) {
        if (request.getPartnerId().equals("") || request.getPartnerId() == null){
        throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "PartnerId cannot be empty");
    }
    }

    public void validateColor(ColorRequestDto request) {
        if (request.getName().equals("") || request.getName() == null){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "name cannot be empty");
        }
        String valName = request.getName();
        char valCharName = valName.charAt(0);
        if (Character.isDigit(valCharName)){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name can not start with a number");
        }
    }

    public void validateFulfilment(FulfilmentDashboardDto request) {
        Warehouse warehouse = warehouseRepository.findWarehouseById(request.getWareHouseId());
        if (warehouse == null){
            throw new ConflictException(CustomResponseCode.NOT_FOUND_EXCEPTION, " warehouse does not exist");

        }
    }

    public void validateDriverWallet(DriverWalletDto request){
        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid driver!"));
        if (request.getAmount() == null || request.getAmount().equals("") )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "amount cannot be empty");
        if (!TransAction.DEPOSIT.equals(request.getAction())  &&  !TransAction.WITHDRAWAL.equals(request.getAction()))
            throw  new BadRequestException(CustomResponseCode.BAD_REQUEST,"please enter a valid action : DEPOSIT OR WITHDRAWAL");
    }

    public void validateWalletTransaction(WalletTransactionDto request){
        DriverWallet driverWallet = driverWalletRepository.findById(request.getDriverWalletId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid driver wallet!"));
//        DriverWallet driverWallet = driverWalletRepository.findById(request.getDriverWalletId())
//                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
//                        " Enter a valid driver wallet!"));
        if (request.getAmount() == null || request.getAmount().equals("") )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "amount cannot be empty");
        if (!TransAction.DEPOSIT.equals(request.getAction())  &&  !TransAction.WITHDRAWAL.equals(request.getAction()))
            throw  new BadRequestException(CustomResponseCode.BAD_REQUEST,"please enter a valid action : DEPOSIT OR WITHDRAWAL");
    }

    public void validateWarehouseProduct(WarehouseProductDto warehouseProductDto) {
        if (warehouseProductDto.getProductName() == null || warehouseProductDto.getProductName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Product Name cannot be empty");
        String valName = warehouseProductDto.getProductName();
        char valCharName = valName.charAt(0);
        if (Character.isDigit(valCharName)){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Product Name can not start with a number");
        }
        if(warehouseProductDto.getThirdPartyProductID() == null || warehouseProductDto.getThirdPartyProductID().isEmpty()){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Thirdparty Product id can not be empty");
        }

        Warehouse warehouse = warehouseRepository.findById(warehouseProductDto.getWarehouseId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid warehouse id!"));
        if (warehouseProductDto.getThirdPartyProductID() == null || warehouseProductDto.getThirdPartyProductID().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Product Name cannot be empty");
        if (warehouseProductDto.getQuantityAvailable() < 1)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "quantity avaliable cannot be less than 1");
        if (warehouseProductDto.getQuantity() < 1)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "quantity cannot be less than 1");
        if (warehouseProductDto.getQuantitySold() < 1)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "quantity sold cannot be less than 1");
    }

    public void validatePricingConfiguration(PricingConfigurationRequest request) {
        partnerRepository.findById(request.getPartnerId()).orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                " Enter a valid partner id!"));
    }

    public void validatePricingConfiguration(PricingConfigMasterRequest request) {
        partnerRepository.findById(request.getPartnerId()).orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                " Enter a valid partner id!"));
    }

    public void validatePricingItem(PricingItemsRequest request) {
        partnerAssetTypeRepository.findById(request.getPartnerAssetTypeId()).orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                " Enter a valid asset type id!"));
        pricingConfigurationRepository.findById(request.getPricingConfigurationId()).orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                " Enter a valid pricing configuration id!"));

    }

    public void validaterouteLocation(RouteLocationRequest request) {
        stateRepository.findById(request.getStateId()).orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                " Enter a valid state id!"));
    }

    public void validaterouteLocationTollPrice(RouteLocationTollPriceRequest request) {
        stateRepository.findById(request.getStateId()).orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                " Enter a valid state id!"));
    }


//    public void validatedashBoard(DashboardSummary dashboardSummary){
//        DashboardSummary dashboard = dashboardSummaryRepository.findByPartnerIdAndReferenceNo(dashboardSummary.getPartnerId(),dashboardSummary.getReferenceNo());
//        if(dashboard ==null)
//            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " DashBoard record already exist");
//    }

    public void validateTollPrices(TollPricesDto request) {
        assetTypePropertiesRepository.findById(request.getAssestTypeId()).orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                " Enter a valid assestType id!"));
        if (request.getRouteLocationId() != null) {
            routeLocationRepository.findById(request.getRouteLocationId()).orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    " Enter a valid route location id!"));
        }
    }


}


