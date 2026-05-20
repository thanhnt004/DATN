package com.example.categoryservice.service.iml;

import com.example.categoryservice.dto.projection.CategoryProjection;
import com.example.categoryservice.dto.request.CategoryCreateRequest;
import com.example.categoryservice.dto.request.CategoryMoveRequest;
import com.example.categoryservice.dto.request.CategoryUpdateRequest;
import com.example.categoryservice.dto.response.CategoryBreadcrumb;
import com.example.categoryservice.dto.response.CategoryResponse;
import com.example.categoryservice.dto.response.CategoryTreeResponse;
import com.example.categoryservice.exception.CategoryBusinessException;
import com.example.categoryservice.exception.CategoryErrorCode;
import com.example.categoryservice.mapper.CategoryMapper;
import com.example.categoryservice.model.Category;
import com.example.categoryservice.repository.CategoryRepository;
import com.example.categoryservice.service.CategoryService;
import com.example.categoryservice.utils.CategorySlugUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceIml implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        UUID newId = UUID.randomUUID();
        Category parent = null;

        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new CategoryBusinessException(CategoryErrorCode.PARENT_CATEGORY_NOT_FOUND));
        }

        String slug = CategorySlugUtil.uniqueCategorySlug(
                request.getName(),
                request.getParentId(),
                categoryRepository::existsByParentIdAndSlug
        );

        Category newCategory = categoryMapper.toEntity(request);
        newCategory.setId(newId);
        newCategory.setSlug(slug);
        newCategory.setParent(parent);

        // Thiết lập Path và Level
        if (parent == null) {
            newCategory.setLevel(1);
            newCategory.setPath(newId + "/");
        } else {
            newCategory.setLevel(parent.getLevel() + 1);
            newCategory.setPath(parent.getPath() + newId + "/");
        }

        // Gán category cho từng attribute (MapStruct không tự set quan hệ ngược)
        if (newCategory.getAttributes() != null) {
            for (var attr : newCategory.getAttributes()) {
                attr.setCategory(newCategory);
                if (attr.getPredefinedValues() != null) {
                    attr.getPredefinedValues().forEach(v -> v.setAttribute(attr));
                }
            }
        }

        // Thiết lập SortOrder (Mặc định xuống cuối danh sách nếu không truyền)
        if (newCategory.getSortOrder() == null) {
            newCategory.setSortOrder(categoryRepository.findNextSortOrder(request.getParentId()));
        }else {
            // Dọn chỗ cho mục mới
            categoryRepository.shiftSortOrder(request.getParentId(), newCategory.getSortOrder(), 1);
        }
        try {
            newCategory = categoryRepository.save(newCategory);
        } catch (DataIntegrityViolationException ex) {
            throw new CategoryBusinessException(CategoryErrorCode.SLUG_ALREADY_EXISTS);
        }

        return toResponseWithBreadcrumb(newCategory);
    }

    @Override
    public List<CategoryTreeResponse> getTree(UUID parentId, Integer depth) {
        List<CategoryProjection> categories;

        if (parentId == null) {
            // Trường hợp lấy từ gốc: chỉ lấy những category có level <= depth
            categories = categoryRepository.findAllByLevelLessThanEqual(depth);
        } else {
            // Trường hợp lấy từ 1 danh mục cha: dựa vào path của cha để tìm con
            Category parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new CategoryBusinessException(CategoryErrorCode.PARENT_CATEGORY_NOT_FOUND));

            int maxLevel = parent.getLevel() + depth;

            // Tìm các category có path bắt đầu bằng path của cha và trong khoảng depth cho phép
            categories = categoryRepository.findChildrenByPath(parent.getPath(), maxLevel);
        }
        List<CategoryTreeResponse> allResponses = categories.stream()
                .map(this::mapToResponse)
                .toList();
        // Xây dựng cấu trúc cây (giữ nguyên logic Map để đạt O(n))
        Map<UUID, CategoryTreeResponse> nodesMap = allResponses.stream()
                .collect(Collectors.toMap(CategoryTreeResponse::getId, node -> node));

        List<CategoryTreeResponse> roots = new ArrayList<>();
        for (CategoryTreeResponse node : allResponses) {
            UUID pId = node.getParentId();
            // Nếu không có parentId, hoặc parent không nằm trong tập dữ liệu lấy về (do giới hạn level), hoặc node chính là node bắt đầu (khi lấy theo parentId)
            if (pId == null || !nodesMap.containsKey(pId) || node.getId().equals(parentId)) {
                roots.add(node);
            } else {
                CategoryTreeResponse parentNode = nodesMap.get(pId);
                if (parentNode.getChildren() == null) {
                    parentNode.setChildren(new ArrayList<>());
                }
                parentNode.getChildren().add(node);
            }
        }

        // Bước 3: Sắp xếp các node con (chỉ cần thiết nếu DB không trả về đúng thứ tự hoặc muốn đảm bảo tuyệt đối)
        // Vì DB đã có ORDER BY level, sortOrder, các node con thường đã được thêm vào cha theo đúng thứ tự.
        // Tuy nhiên, sortChildrenRecursive đảm bảo thứ tự sortOrder trên từng cấp.
        sortChildrenRecursive(roots);
        return roots;
    }
    private void sortChildrenRecursive(List<CategoryTreeResponse> nodes) {
        if (nodes == null || nodes.isEmpty()) return;

        // Sắp xếp các node ở cấp hiện tại
        nodes.sort(Comparator.comparing(CategoryTreeResponse::getSortOrder));

        // Đệ quy xuống các con
        for (CategoryTreeResponse node : nodes) {
            sortChildrenRecursive(node.getChildren());
        }
    }

    private CategoryTreeResponse mapToResponse(CategoryProjection category) {
        return CategoryTreeResponse.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .name(category.getName())
                .path(category.getPath())
                .slug(category.getSlug())
                .level(category.getLevel())
                .imageUrl(category.getImageUrl())
                .iconUrl(category.getIconUrl())
                .sortOrder(category.getSortOrder())
                .status(category.getStatus())
                .description(category.getDescription())
                .build();
    }


    @Override
    @Transactional
    public CategoryResponse updateCategory(CategoryUpdateRequest request, UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryBusinessException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        UUID oldParentId = category.getParent() == null ? null : category.getParent().getId();
        // Nếu request không gửi parentId thì giữ nguyên cha hiện tại
        UUID newParentId = request.getParentId() != null ? request.getParentId() : oldParentId;
        Integer oldSortOrder = category.getSortOrder();
        Integer newSortOrder = request.getSortOrder();

        // 1. Cập nhật các thông tin cơ bản (Name, Description...)
        categoryMapper.updateEntityFromDto(request, category);

        // 2. Xử lý thay đổi cấu trúc Cha (Move Branch)
        if (!Objects.equals(oldParentId, newParentId)) {
            validateHierarchy(id, newParentId, category.getPath());

            if (newSortOrder == null) {
                newSortOrder = categoryRepository.findNextSortOrder(newParentId);
            }

            // Giải phóng vị trí ở cha cũ và tạo chỗ ở cha mới
            categoryRepository.shiftSortOrder(oldParentId, oldSortOrder + 1, -1);
            categoryRepository.shiftSortOrder(newParentId, newSortOrder, 1);

            category.setSortOrder(newSortOrder);

            // Lưu thông số cũ để update con cháu
            String oldPath = category.getPath();
            int oldLevel = category.getLevel();

            if (newParentId == null) {
                category.setParent(null);
                category.setPath(id + "/");
                category.setLevel(1);
            } else {
                Category newParent = categoryRepository.findById(newParentId)
                        .orElseThrow(() -> new CategoryBusinessException(CategoryErrorCode.PARENT_CATEGORY_NOT_FOUND));
                category.setParent(newParent);
                category.setPath(newParent.getPath() + id + "/");
                category.setLevel(newParent.getLevel() + 1);
            }

            // Đồng bộ toàn bộ con cháu bằng 1 câu lệnh SQL duy nhất
            categoryRepository.updateChildPaths(oldPath, category.getPath(), oldLevel, category.getLevel());
        }
        // 3. Xử lý chỉ thay đổi thứ tự trong cùng một cha
        else if (newSortOrder != null && !Objects.equals(oldSortOrder, newSortOrder)) {
            if (newSortOrder < oldSortOrder) {
                categoryRepository.reorderRange(oldParentId, newSortOrder, oldSortOrder - 1, 1);
            } else {
                categoryRepository.reorderRange(oldParentId, oldSortOrder + 1, newSortOrder, -1);
            }
            category.setSortOrder(newSortOrder);
        }

        return toResponseWithBreadcrumb(categoryRepository.save(category));
    }

    /**
     * Chuyển đổi sang Response và tính toán Breadcrumb từ Path
     */
    private CategoryResponse toResponseWithBreadcrumb(Category category) {
        CategoryResponse response = categoryMapper.toDto(category);

        // Tính toán Breadcrumb dựa trên Path (VD: "uuid1/uuid2/uuid3/")
        String[] pathIds = category.getPath().split("/");
        List<UUID> breadcrumbIds = Arrays.stream(pathIds)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .toList();

        // Lấy thông tin các cha từ DB (hoặc Cache nếu có)
        List<Category> parents = categoryRepository.findAllById(breadcrumbIds);

        // Sắp xếp lại theo đúng thứ tự của Path
        Map<UUID, Category> categoryMap = new HashMap<>();
        parents.forEach(p -> categoryMap.put(p.getId(), p));

        List<CategoryBreadcrumb> breadcrumbs = breadcrumbIds.stream()
                .map(categoryMap::get)
                .filter(Objects::nonNull)
                .map(p -> new CategoryBreadcrumb(p.getId(), p.getName(), p.getSlug(), p.getLevel()))
                .toList();

        response.setBreadcrumbs(breadcrumbs);
        return response;
    }

    private void validateHierarchy(UUID id, UUID newParentId, String currentPath) {
        if (Objects.equals(newParentId, id)) {
            throw new CategoryBusinessException(CategoryErrorCode.SELF_PARENTING);
        }
        if (newParentId != null && isChildOf(newParentId, currentPath)) {
            throw new CategoryBusinessException(CategoryErrorCode.CANNOT_SET_CHILD_AS_PARENT);
        }
    }

    private boolean isChildOf(UUID newParentId, String path) {
        return path.contains(newParentId.toString());
    }

    @Override
    public CategoryResponse getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryBusinessException(CategoryErrorCode.CATEGORY_NOT_FOUND));
        return toResponseWithBreadcrumb(category);
    }

    @Override
    @Transactional
    public CategoryResponse moveCategory(UUID id, CategoryMoveRequest request) {
        // 1. Lấy thực thể cần di chuyển
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryBusinessException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        UUID oldParentId = category.getParent() == null ? null : category.getParent().getId();
        UUID newParentId = request.getParentId();
        Integer oldSortOrder = category.getSortOrder();
        Integer newSortOrder = request.getSortOrder();

        // 2. Kiểm tra logic phân cấp (Validation)
        validateHierarchy(id,newParentId, category.getPath());

        // 3. Xử lý SortOrder
        if (newSortOrder == null) {
            newSortOrder = categoryRepository.findNextSortOrder(newParentId);
        }

        // Trường hợp A: Di chuyển sang cha khác
        if (!Objects.equals(oldParentId, newParentId)) {
            // Rút khỏi cha cũ: các anh em cũ phía sau lùi lại 1 bước
            categoryRepository.shiftSortOrder(oldParentId, oldSortOrder + 1, -1);

            // Chèn vào cha mới: các anh em mới từ vị trí newSortOrder tiến lên 1 bước
            categoryRepository.shiftSortOrder(newParentId, newSortOrder, 1);

            // Cập nhật thông tin bản thân
            String oldPath = category.getPath();
            int oldLevel = category.getLevel();

            if (newParentId == null) {
                category.setParent(null);
                category.setPath(id + "/");
                category.setLevel(1);
            } else {
                Category newParent = categoryRepository.findById(newParentId).orElseThrow();
                category.setParent(newParent);
                category.setPath(newParent.getPath() + id + "/");
                category.setLevel(newParent.getLevel() + 1);
            }
            category.setSortOrder(newSortOrder);

            // Đồng bộ con cháu (Quan trọng nhất)
            categoryRepository.updateChildPaths(oldPath, category.getPath(), oldLevel, category.getLevel());
        }
        // Trường hợp B: Chỉ đổi thứ tự trong cùng một cha
        else if (!Objects.equals(oldSortOrder, newSortOrder)) {
            if (newSortOrder < oldSortOrder) {
                // Kéo lên
                categoryRepository.reorderRange(oldParentId, newSortOrder, oldSortOrder - 1, 1);
            } else {
                // Kéo xuống
                categoryRepository.reorderRange(oldParentId, oldSortOrder + 1, newSortOrder, -1);
            }
            category.setSortOrder(newSortOrder);
        }

        return toResponseWithBreadcrumb(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryBusinessException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        UUID parentId = category.getParent() == null ? null : category.getParent().getId();
        Integer sortOrder = category.getSortOrder();

        // Xoá category (cascade xoá children + attributes)
        categoryRepository.delete(category);

        // Dồn sortOrder lại cho các category cùng cấp phía sau
        categoryRepository.shiftSortOrder(parentId, sortOrder + 1, -1);
    }
}
