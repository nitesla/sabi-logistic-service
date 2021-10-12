package com.sabilogistics.service.helper;



import com.sabi.framework.exceptions.BadRequestException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabilogistics.service.repositories.LGARepository;
import com.sabilogistics.service.repositories.StateRepository;
import com.sabilogisticscore.dto.request.*;
import com.sabilogisticscore.models.LGA;
import com.sabilogisticscore.models.State;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@SuppressWarnings("All")
@Slf4j
@Service
public class Validations {


    private StateRepository stateRepository;
    private LGARepository lgaRepository;

    public Validations(StateRepository stateRepository,LGARepository lgaRepository) {
        this.stateRepository = stateRepository;
        this.lgaRepository = lgaRepository;

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
}
