package com.sabi.logistics.service.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.logistics.core.dto.request.CategoryDto;
import com.sabi.logistics.core.dto.response.CategoryResponseDto;
import com.sabi.logistics.core.models.Category;
import com.sabi.logistics.service.helper.Validations;
import com.sabi.logistics.service.repositories.CategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class CategoryService {

    private CategoryRepository categoryRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;

    public CategoryService(CategoryRepository categoryRepository, ModelMapper mapper, ObjectMapper objectMapper,Validations validations) {
        this.categoryRepository = categoryRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
    }




    public CategoryResponseDto createCategory(CategoryDto request) {
        validations.validateCategory(request);
        Category category = mapper.map(request,Category.class);
        Category categoryExist = categoryRepository.findByName(request.getName());
        if(categoryExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Category already exist");
        }
        category.setCreatedBy(0l);
        category.setActive(true);
        category = categoryRepository.save(category);
        log.debug("Create new category - {}"+ new Gson().toJson(category));
        return mapper.map(category, CategoryResponseDto.class);
    }

    public CategoryResponseDto updateCategory(CategoryDto request) {
        validations.validateCategory(request);
        Category category = categoryRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested category Id does not exist!"));
        mapper.map(request, category);
        category.setUpdatedBy(0l);
        categoryRepository.save(category);
        log.debug("Category record updated - {}"+ new Gson().toJson(category));
        return mapper.map(category, CategoryResponseDto.class);
    }


    public CategoryResponseDto findCategory(Long id){
        Category category  = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Category Id does not exist!"));
        return mapper.map(category,CategoryResponseDto.class);
    }


    public Page<Category> findAll(String name, PageRequest pageRequest ){
        Page<Category> categories = categoryRepository.findCategories(name,pageRequest);
        if(categories == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return categories;

    }



    public void enableDisEnableState (EnableDisEnableDto request){
        Category category  = categoryRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Category Id does not exist!"));
        category.setActive(request.isActive());
        category.setUpdatedBy(0l);
        categoryRepository.save(category);

    }


    public List<Category> getAll(Boolean isActive){
        List<Category> categories = categoryRepository.findByIsActive(isActive);
        return categories;

    }




}
