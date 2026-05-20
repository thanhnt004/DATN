package com.example.categoryservice.service.iml;

import com.example.categoryservice.dto.request.CategoryAttributeRequest;
import com.example.categoryservice.dto.response.CategoryAttributeResponse;
import com.example.categoryservice.exception.CategoryBusinessException;
import com.example.categoryservice.exception.CategoryErrorCode;
import com.example.categoryservice.mapper.CategoryAttributeMapper;
import com.example.categoryservice.model.Category;
import com.example.categoryservice.model.CategoryAttribute;
import com.example.categoryservice.repository.CategoryRepository;
import com.example.categoryservice.repository.CategoryValueRepository;
import com.example.categoryservice.service.CategoryAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import response.PageResponse;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryAttributeServiceIml implements CategoryAttributeService {
    private final CategoryRepository categoryRepository;
    private final CategoryValueRepository categoryValueRepository;
    private final CategoryAttributeMapper categoryAttributeMapper;
    @Override
    public List<CategoryAttributeResponse> getAttributesByCategoryId(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(()-> new CategoryBusinessException(CategoryErrorCode.CATEGORY_NOT_FOUND));
        List<CategoryAttribute> categoryAttributes = category.getAttributes();
        return categoryAttributeMapper.toDtoList(categoryAttributes);
    }

    @Override
    @Transactional
    public CategoryAttributeResponse createCategoryAttribute(UUID categoryId, CategoryAttributeRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryBusinessException(CategoryErrorCode.CATEGORY_NOT_FOUND));
        CategoryAttribute attr = categoryAttributeMapper.toEntity(request);
        attr.setCategory(category);
        if (attr.getPredefinedValues() != null) {
            attr.getPredefinedValues().forEach(v -> v.setAttribute(attr));
        }
        categoryValueRepository.save(attr);
        return categoryAttributeMapper.toDto(attr);
    }

    @Override
    public CategoryAttributeResponse updateCategoryAttribute(UUID attributeId, CategoryAttributeRequest request) {
        CategoryAttribute categoryAttribute = categoryValueRepository.findById(attributeId).orElseThrow(()-> new CategoryBusinessException(CategoryErrorCode.ATTRIBUTE_NOT_FOUND));
        categoryAttributeMapper.updateEntityFromDto(request, categoryAttribute);
        categoryValueRepository.save(categoryAttribute);
        return categoryAttributeMapper.toDto(categoryAttribute);
    }

    @Override
    @Transactional
    public void deleteCategoryAttribute(UUID attributeId) {
        CategoryAttribute attr = categoryValueRepository.findById(attributeId)
                .orElseThrow(() -> new CategoryBusinessException(CategoryErrorCode.ATTRIBUTE_NOT_FOUND));
        categoryValueRepository.delete(attr);
    }

}
