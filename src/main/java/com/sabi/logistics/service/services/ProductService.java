package com.sabi.logistics.service.services;

import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.ProductRequestDto;
import com.sabi.logistics.core.dto.response.ProductResponseDto;
import com.sabi.logistics.core.models.Product;
import com.sabi.logistics.service.helper.GenericSpecification;
import com.sabi.logistics.service.helper.SearchCriteria;
import com.sabi.logistics.service.helper.SearchOperation;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@SuppressWarnings("All")
@Service
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    private final ModelMapper mapper;
    @Autowired
    private Validations validations;


    public ProductService(ProductRepository productRepository, ModelMapper mapper) {
        this.productRepository = productRepository;
        this.mapper = mapper;
    }

    public ProductResponseDto createProduct(ProductRequestDto request) {
        validations.validateProduct(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Product product = mapper.map(request,Product.class);

        Product productExists = productRepository.findByNameAndThirdPartyId(product.getName(), product.getThirdPartyId());

        if(productExists != null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Product already exist");
        }
        product.setCreatedBy(userCurrent.getId());
        product.setIsActive(true);
        product = productRepository.save(product);
        log.debug("Create new tripRequestResponse - {}"+ new Gson().toJson(product));
        ProductResponseDto productResponseDto =  mapper.map(product, ProductResponseDto.class);
        productResponseDto.setStockLeft(productResponseDto.getTotalStock() - productResponseDto.getStockSold());
        return productResponseDto;

    }

    public ProductResponseDto updateProduct(ProductRequestDto request) {
        validations.validateProduct(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Product product = productRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested product Id does not exist!"));
        mapper.map(request, product);

        product.setUpdatedBy(userCurrent.getId());
        productRepository.save(product);
        log.debug("product record updated - {}"+ new Gson().toJson(product));
        ProductResponseDto productResponseDto =  mapper.map(product, ProductResponseDto.class);
        productResponseDto.setStockLeft(productResponseDto.getTotalStock() - productResponseDto.getStockSold());
        return productResponseDto;

    }

    public ProductResponseDto findProduct(Long id){
        Product product  = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested productId does not exist!"));
        ProductResponseDto productResponseDto =  mapper.map(product, ProductResponseDto.class);
        productResponseDto.setStockLeft(productResponseDto.getTotalStock() - productResponseDto.getStockSold());
        return productResponseDto;
    }


    public Page<Product> findAll(Long thirdPartyId, String name, Double totalStock, Double stockSold, PageRequest pageRequest ){
        GenericSpecification<Product> genericSpecification = new GenericSpecification<Product>();

        if (thirdPartyId != null)
        {
            genericSpecification.add(new SearchCriteria("thirdPartyId", thirdPartyId, SearchOperation.EQUAL));
        }

        if (totalStock != null)
        {
            genericSpecification.add(new SearchCriteria("totalStock", totalStock, SearchOperation.EQUAL));
        }

        if (stockSold != null )
        {
            genericSpecification.add(new SearchCriteria("stockSold", stockSold, SearchOperation.EQUAL));
        }



        Page<Product> products = productRepository.findAll(genericSpecification, pageRequest);
        if(products == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return products;

    }



    public void enableDisable (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Product product  = productRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Product Id does not exist!"));
        product.setIsActive(request.isActive());
        product.setUpdatedBy(userCurrent.getId());
        productRepository.save(product);

    }


    public List<Product> getAll(Long thirdPartyId, Boolean isActive){
        List<Product> products = productRepository.findByThirdPartyIdAndIsActive(thirdPartyId, isActive);
        return products;

    }
}
