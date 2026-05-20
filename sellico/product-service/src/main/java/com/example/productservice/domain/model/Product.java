package com.example.productservice.domain.model;

import com.example.productservice.domain.exception.ProductBusinessException;
import com.example.productservice.domain.exception.ProductErrorCode;
import lombok.*;
import model.SpecAttribute;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Product {
    private UUID id;
    private UUID sellerId;
    private UUID categoryId;
    private String name;
    private String slug;
    private String description;
    private String status;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private Integer soldCount;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean isDeleted;
    private Instant createdAt;
    private Instant updatedAt;

    // Thuộc tính từ ProductSpecification (JSONB)
    private List<SpecAttribute> specifications;

    private List<ProductImage> images;
    private List<ProductOption> options;
    private List<ProductSku> skus;

    // Logic nghiệp vụ: Ví dụ kiểm tra xem sản phẩm có hợp lệ để đăng bán không
    public boolean isReadyToPublish() {
        return name != null && !skus.isEmpty() && !images.isEmpty();
    }

    public static Product createNew(
            UUID sellerId,
            UUID categoryId,
            String name,
            String description,
            List<SpecAttribute> specifications,
            List<ProductImage> images,
            List<ProductOption> options,
            List<ProductSku> skus
    ) {
        Instant now = Instant.now();

        Product product = Product.builder()
                .id(UUID.randomUUID())
                .sellerId(sellerId)
                .categoryId(categoryId)
                .name(name)
                .slug(slugify(name))
                .description(description)
                .status("DRAFT")
                .ratingAvg(null)
                .ratingCount(0)
                .soldCount(0)
                .minPrice(calcMinPrice(skus))
                .maxPrice(calcMaxPrice(skus))
                .isDeleted(false)
                .createdAt(now)
                .updatedAt(now)
                .specifications(specifications)
                .images(images)
                .options(options)
                .skus(skus)
                .build();

        return product;
    }

    public void recalcPriceRange() {
        this.minPrice = calcMinPrice(this.skus);
        this.maxPrice = calcMaxPrice(this.skus);
        this.updatedAt = Instant.now();
    }

    private static BigDecimal calcMinPrice(List<ProductSku> skus) {
        return skus.stream()
                .map(ProductSku::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private static BigDecimal calcMaxPrice(List<ProductSku> skus) {
        return skus.stream()
                .map(ProductSku::getPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private static String slugify(String input) {
        return input.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-");
    }

    public void clearPrimaryImage() {
        if (images != null) {
            for (ProductImage image : images) {
                image.setIsPrimary(false);
            }
        }
    }

    public void addImage(ProductImage image) {
        if (this.images == null) {
            this.images = new ArrayList<>();
        }

        // Nếu là primary → clear cái cũ
        if (image.getIsPrimary()) {
            this.images.forEach(img -> img.setIsPrimary(false));
        }

        int index = image.getSortOrder() == null ? images.size() : image.getSortOrder();
        index = Math.max(0, Math.min(index, images.size()));

        // dời sortOrder các ảnh phía sau
        for (int i = index; i < images.size(); i++) {
            ProductImage img = images.get(i);
            img.setSortOrder(img.getSortOrder() + 1);
        }

        image.setSortOrder(index);
        this.images.add(index, image);
    }

    public void removeImage(UUID imageId) {
        if (this.images == null || this.images.isEmpty()) {
            return;
        }

        ProductImage removed = null;

        Iterator<ProductImage> it = images.iterator();
        while (it.hasNext()) {
            ProductImage img = it.next();
            if (img.getId().equals(imageId)) {
                removed = img;
                it.remove();
                break;
            }
        }

        if (removed == null) {
            throw new ProductBusinessException(ProductErrorCode.IMAGE_NOT_FOUND);
        };
        // Sắp lại sortOrder
        for(int i = 0; i < images.size(); i++)
        {
            images.get(i).setSortOrder(i);
        }

        // Nếu xoá ảnh primary → set primary cho ảnh đầu tiên
        if(removed.getIsPrimary()&&!images.isEmpty())
        {
            images.get(0).setIsPrimary(true);
        }
    }


    public void setPrimaryImage(UUID imageId) {
        if (this.images == null || this.images.isEmpty()) {
            throw new ProductBusinessException(ProductErrorCode.PRODUCT_IMAGE_REQUIRED);
        }

        boolean found = false;
        for (ProductImage img : images) {
            if (img.getId().equals(imageId)) {
                img.setIsPrimary(true);
                found = true;
            } else {
                img.setIsPrimary(false);
            }
        }

        if (!found) {
            throw new ProductBusinessException(ProductErrorCode.IMAGE_NOT_FOUND);
        }

        this.updatedAt = Instant.now();
    }
}
