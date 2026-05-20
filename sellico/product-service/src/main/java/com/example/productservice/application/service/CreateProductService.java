package com.example.productservice.application.service;

import com.example.productservice.application.command.CreateProductCommand;
import com.example.productservice.application.mapper.ProductMapper;
import com.example.productservice.application.port.in.CreateProductUseCase;
import com.example.productservice.application.port.out.CategoryClientPort;
import com.example.productservice.application.port.out.InventoryClientPort;
import com.example.productservice.application.port.out.ProductEventPublisherPort;
import com.example.productservice.application.port.out.ProductRepositoryPort;
import com.example.productservice.application.port.out.SellerClientPort;
import com.example.productservice.application.utils.ProductSlugUtil;
import com.example.productservice.domain.exception.ProductBusinessException;
import com.example.productservice.domain.exception.ProductErrorCode;
import com.example.productservice.domain.model.Product;
import com.example.productservice.domain.model.ProductSku;
import com.example.productservice.domain.service.ProductDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreateProductService implements CreateProductUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final CategoryClientPort categoryClientPort;
    private final SellerClientPort sellerClientPort;
    private final ProductDomainService productDomainService;
    private final ProductEventPublisherPort eventPublisher;
    private final InventoryClientPort inventoryClientPort;
    @Override
    @Transactional
    public Product createProduct(CreateProductCommand command) {
        //validate category
        //existed
        if (!categoryClientPort.isExist(command.getCategoryId()))
        {
            throw new ProductBusinessException(ProductErrorCode.CATEGORY_NOT_EXISTED);
        }
        if (!categoryClientPort.isLeaf(command.getCategoryId()))
        {
            throw new ProductBusinessException(ProductErrorCode.CATEGORY_IS_NOT_LEAF);
        }
        //seller
        if (!sellerClientPort.isSellerActive(command.getSellerId()))
        {
            throw new ProductBusinessException(ProductErrorCode.SELLER_ID_INVALID);
        }

        //Khởi tạo Domain Model từ Command
        Product product = ProductMapper.toDomain(command);
        String slug = ProductSlugUtil.uniqueProductSlug(
                product.getName(),
                product.getSellerId(),
                productRepositoryPort::existsBySellerIdAndSlug,
                150, // max length nếu muốn
                "-"
        );

        product.setSlug(slug);

        // Auto-generate unique SKU codes
        ensureUniqueSkuCodes(product);

        // validate nghiep vu
        productDomainService.validateNewProduct(product);

        Product saved = productRepositoryPort.save(product);
        eventPublisher.publishProductCreated(saved);

        // Sync inventory for SKUs via Feign client
        syncInventoryForSkus(saved, command);

        return saved;
    }
    /**
     * Ensures every SKU has a unique code.
     * - If skuCode is null/blank, auto-generate one.
     * - If skuCode already exists for this seller in DB, regenerate with a new random suffix.
     * - Also deduplicate within the current batch.
     */
    private void ensureUniqueSkuCodes(Product product) {
        UUID sellerId = product.getSellerId();
        String namePrefix = product.getName()
                .replaceAll("[^a-zA-Z0-9\\u00C0-\\u1EF9]", "")
                .substring(0, Math.min(6, product.getName().replaceAll("[^a-zA-Z0-9\\u00C0-\\u1EF9]", "").length()))
                .toUpperCase();
        if (namePrefix.isBlank()) namePrefix = "SKU";

        Set<String> usedCodes = new HashSet<>();

        // First pass: generate codes for blank ones and collect all codes
        for (int i = 0; i < product.getSkus().size(); i++) {
            ProductSku sku = product.getSkus().get(i);
            if (sku.getSkuCode() == null || sku.getSkuCode().isBlank()) {
                sku.setSkuCode(generateSkuCode(namePrefix, i));
            }
            // Deduplicate within batch
            while (!usedCodes.add(sku.getSkuCode())) {
                sku.setSkuCode(generateSkuCode(namePrefix, i));
            }
        }

        // Second pass: check against DB and regenerate if collision
        Set<String> existingCodes = productRepositoryPort.findExistingSkuCodesBySellerId(sellerId, usedCodes);
        if (!existingCodes.isEmpty()) {
            for (ProductSku sku : product.getSkus()) {
                int attempts = 0;
                while (existingCodes.contains(sku.getSkuCode()) && attempts < 10) {
                    String newCode = generateSkuCode(namePrefix, attempts);
                    if (!usedCodes.contains(newCode)) {
                        usedCodes.remove(sku.getSkuCode());
                        sku.setSkuCode(newCode);
                        usedCodes.add(newCode);
                    }
                    attempts++;
                }
            }
            // Final validation
            Set<String> finalCodes = product.getSkus().stream().map(ProductSku::getSkuCode).collect(Collectors.toSet());
            Set<String> stillExisting = productRepositoryPort.findExistingSkuCodesBySellerId(sellerId, finalCodes);
            if (!stillExisting.isEmpty()) {
                throw new ProductBusinessException(ProductErrorCode.SKU_CODE_ALREADY_EXISTS_FOR_SELLER);
            }
        }
    }

    private String generateSkuCode(String prefix, int index) {
        String rand = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + "-" + String.format("%03d", index + 1) + "-" + rand;
    }

    private void syncInventoryForSkus(Product saved, CreateProductCommand command) {
        List<ProductSku> savedSkus = saved.getSkus();
        List<CreateProductCommand.SkuCommand> skuCommands = command.getSkus();

        if (savedSkus == null || savedSkus.isEmpty()) return;

        List<InventoryClientPort.SkuInventoryInfo> inventoryInfos = new java.util.ArrayList<>();
        for (int i = 0; i < savedSkus.size(); i++) {
            ProductSku savedSku = savedSkus.get(i);
            // Match by index - SKUs are created in the same order as the command
            CreateProductCommand.SkuCommand skuCmd = (i < skuCommands.size()) ? skuCommands.get(i) : null;

            int totalStock = (skuCmd != null && skuCmd.getTotalStock() != null) ? skuCmd.getTotalStock() : 0;
            int lowStockThreshold = (skuCmd != null && skuCmd.getLowStockThreshold() != null) ? skuCmd.getLowStockThreshold() : 5;
            String locationCode = (skuCmd != null) ? skuCmd.getLocationCode() : null;

            inventoryInfos.add(new InventoryClientPort.SkuInventoryInfo(
                    savedSku.getId(), totalStock, lowStockThreshold, locationCode
            ));
        }

        inventoryClientPort.createInventoryForSkus(inventoryInfos);
    }

}
