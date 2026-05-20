package com.example.cartservice.service;

import com.example.cartservice.client.InventoryClient;
import com.example.cartservice.client.ProductClient;
import com.example.cartservice.client.SellerClient;
import com.example.cartservice.client.dto.InventoryInfo;
import com.example.cartservice.client.dto.SellerInfo;
import com.example.cartservice.client.dto.SkuInfo;
import com.example.cartservice.dto.request.*;
import com.example.cartservice.dto.response.*;
import com.example.cartservice.entity.*;
import com.example.cartservice.exception.CartErrorCode;
import com.example.cartservice.exception.CartException;
import com.example.cartservice.mapper.CartMapper;
import com.example.cartservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import response.ApiResponse;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private static final int MAX_QUANTITY_PER_ITEM = 99;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final SavedItemRepository savedItemRepository;
    private final CartMapper mapper;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final SellerClient sellerClient;

    // =====================================================
    // Cart Operations
    // =====================================================

    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        Cart cart = getOrCreateCart(userId);
        CartResponse response = mapper.toCartResponse(cart);
        enrichCartItems(response.getItems());
        return response;
    }

    @Transactional
    public CartResponse addToCart(UUID userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);

        // Check inventory availability before adding
        int requestedQty = request.getQuantity();
        Optional<CartItem> existingCartItem = cartItemRepository.findByCart_IdAndSkuId(cart.getId(), request.getSkuId());
        if (existingCartItem.isPresent()) {
            requestedQty += existingCartItem.get().getQuantity();
        }
        try {
            Map<UUID, InventoryInfo> inventoryMap = fetchInventoryBatch(List.of(request.getSkuId()));
            InventoryInfo inv = inventoryMap.get(request.getSkuId());
            if (inv != null) {
                if (!Boolean.TRUE.equals(inv.getIsAvailable()) || inv.getAvailableStock() <= 0) {
                    throw new CartException(CartErrorCode.INSUFFICIENT_STOCK, "Sản phẩm đã hết hàng");
                }
                if (requestedQty > inv.getAvailableStock()) {
                    throw new CartException(CartErrorCode.INSUFFICIENT_STOCK,
                            "Chỉ còn " + inv.getAvailableStock() + " sản phẩm có sẵn");
                }
            }
        } catch (CartException e) {
            throw e; // re-throw our own exceptions
        } catch (Exception e) {
            log.warn("Could not verify inventory for SKU {}: {}", request.getSkuId(), e.getMessage());
        }

        // Fetch real price from product-service
        BigDecimal actualPrice = BigDecimal.ZERO;
        try {
            Map<UUID, SkuInfo> skuInfoMap = fetchSkuInfoBatch(List.of(request.getSkuId()));
            SkuInfo skuInfo = skuInfoMap.get(request.getSkuId());
            if (skuInfo != null && skuInfo.getPrice() != null) {
                actualPrice = skuInfo.getPrice();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch price for SKU {}: {}", request.getSkuId(), e.getMessage());
        }

        // Check if item already exists
        Optional<CartItem> existingItem = existingCartItem;

        if (existingItem.isPresent()) {
            // Update quantity
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + request.getQuantity();

            if (newQuantity > MAX_QUANTITY_PER_ITEM) {
                throw new CartException(CartErrorCode.MAX_QUANTITY_EXCEEDED,
                        "Maximum quantity per item is " + MAX_QUANTITY_PER_ITEM);
            }

            item.setQuantity(newQuantity);
            // Update price in case it changed
            if (actualPrice.compareTo(BigDecimal.ZERO) > 0) {
                item.setPrice(actualPrice);
            }
            cartItemRepository.save(item);
        } else {
            // Add new item
            if (request.getQuantity() > MAX_QUANTITY_PER_ITEM) {
                throw new CartException(CartErrorCode.MAX_QUANTITY_EXCEEDED,
                        "Maximum quantity per item is " + MAX_QUANTITY_PER_ITEM);
            }

            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .skuId(request.getSkuId())
                    .productId(request.getProductId())
                    .sellerId(request.getSellerId())
                    .quantity(request.getQuantity())
                    .price(actualPrice)
                    .selected(true)
                    .build();

            cart.addItem(newItem);
            cartRepository.save(cart);
        }

        log.info("Added item to cart: userId={}, skuId={}, quantity={}",
                userId, request.getSkuId(), request.getQuantity());

        return mapper.toCartResponse(cartRepository.findByUserIdWithItems(userId).orElse(cart));
    }

    @Transactional
    public CartResponse updateCartItem(UUID userId, UUID skuId, UpdateCartItemRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartException(CartErrorCode.CART_NOT_FOUND));

        CartItem item = cartItemRepository.findByCart_IdAndSkuId(cart.getId(), skuId)
                .orElseThrow(() -> new CartException(CartErrorCode.ITEM_NOT_FOUND));

        if (request.getQuantity() > MAX_QUANTITY_PER_ITEM) {
            throw new CartException(CartErrorCode.MAX_QUANTITY_EXCEEDED,
                    "Maximum quantity per item is " + MAX_QUANTITY_PER_ITEM);
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        log.info("Updated cart item: userId={}, skuId={}, quantity={}",
                userId, skuId, request.getQuantity());

        return mapper.toCartResponse(cartRepository.findByUserIdWithItems(userId).orElse(cart));
    }

    @Transactional
    public CartResponse removeFromCart(UUID userId, UUID skuId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartException(CartErrorCode.CART_NOT_FOUND));

        CartItem item = cartItemRepository.findByCart_IdAndSkuId(cart.getId(), skuId)
                .orElseThrow(() -> new CartException(CartErrorCode.ITEM_NOT_FOUND));

        cart.removeItem(item);
        cartItemRepository.delete(item);

        log.info("Removed item from cart: userId={}, skuId={}", userId, skuId);

        return mapper.toCartResponse(cartRepository.findByUserIdWithItems(userId).orElse(cart));
    }

    @Transactional
    public CartResponse removeMultipleItems(UUID userId, RemoveItemsRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartException(CartErrorCode.CART_NOT_FOUND));

        cartItemRepository.deleteByCartIdAndSkuIds(cart.getId(), request.getSkuIds());

        log.info("Removed {} items from cart: userId={}", request.getSkuIds().size(), userId);

        return mapper.toCartResponse(cartRepository.findByUserIdWithItems(userId).orElse(cart));
    }

    @Transactional
    public CartResponse clearCart(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartException(CartErrorCode.CART_NOT_FOUND));

        cart.clearItems();
        cartRepository.save(cart);

        log.info("Cleared cart: userId={}", userId);

        return mapper.toCartResponse(cart);
    }

    // =====================================================
    // Selection Operations
    // =====================================================

    @Transactional
    public CartResponse updateSelection(UUID userId, UpdateSelectionRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartException(CartErrorCode.CART_NOT_FOUND));

        if (request.getSkuIds() != null && !request.getSkuIds().isEmpty()) {
            // Update specific items
            for (UUID skuId : request.getSkuIds()) {
                CartItem item = cartItemRepository.findByCart_IdAndSkuId(cart.getId(), skuId)
                        .orElse(null);
                if (item != null) {
                    item.setSelected(request.getSelected());
                    cartItemRepository.save(item);
                }
            }
        } else if (request.getSellerId() != null) {
            // Update all items from a specific seller
            cartItemRepository.updateSelectionBySeller(cart.getId(), request.getSellerId(), request.getSelected());
        } else {
            // Update all items
            cartItemRepository.updateAllSelection(cart.getId(), request.getSelected());
        }

        return mapper.toCartResponse(cartRepository.findByUserIdWithItems(userId).orElse(cart));
    }

    @Transactional
    public CartResponse selectItem(UUID userId, UUID skuId, boolean selected) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartException(CartErrorCode.CART_NOT_FOUND));

        CartItem item = cartItemRepository.findByCart_IdAndSkuId(cart.getId(), skuId)
                .orElseThrow(() -> new CartException(CartErrorCode.ITEM_NOT_FOUND));

        item.setSelected(selected);
        cartItemRepository.save(item);

        return mapper.toCartResponse(cartRepository.findByUserIdWithItems(userId).orElse(cart));
    }

    // =====================================================
    // Cart By Seller (grouped view)
    // =====================================================

    @Transactional(readOnly = true)
    public List<CartBySellerResponse> getCartGroupedBySeller(UUID userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId).orElse(null);

        if (cart == null || cart.getItems().isEmpty()) {
            return List.of();
        }

        // Enrich all items at once
        List<CartItemResponse> allItemResponses = mapper.toCartItemResponseList(cart.getItems());
        enrichCartItems(allItemResponses);

        // Build a lookup: skuId → enriched response
        Map<UUID, CartItemResponse> responseBySkuId = allItemResponses.stream()
                .collect(Collectors.toMap(CartItemResponse::getSkuId, Function.identity()));

        Map<UUID, List<CartItem>> itemsBySeller = cart.getItems().stream()
                .collect(Collectors.groupingBy(CartItem::getSellerId));

        // Fetch seller names in batch
        Map<UUID, String> sellerNameMap = fetchSellerNames(new ArrayList<>(itemsBySeller.keySet()));

        return itemsBySeller.entrySet().stream()
                .map(entry -> {
                    UUID sellerId = entry.getKey();
                    List<CartItem> items = entry.getValue();
                    List<CartItemResponse> itemResponses = items.stream()
                            .map(i -> responseBySkuId.get(i.getSkuId()))
                            .filter(Objects::nonNull)
                            .toList();

                    BigDecimal subtotal = itemResponses.stream()
                            .map(CartItemResponse::getSubtotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    boolean allSelected = itemResponses.stream()
                            .allMatch(r -> Boolean.TRUE.equals(r.getSelected()));

                    String sellerName = sellerNameMap.get(sellerId);

                    return CartBySellerResponse.builder()
                            .sellerId(sellerId)
                            .sellerName(sellerName)
                            .items(itemResponses)
                            .subtotal(subtotal)
                            .itemCount(items.size())
                            .allSelected(allSelected)
                            .build();
                })
                .toList();
    }

    // =====================================================
    // Saved For Later (Wishlist)
    // =====================================================

    @Transactional
    public SavedItemResponse saveForLater(UUID userId, UUID skuId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);

        // Get item info from cart if exists
        UUID productId = null;
        if (cart != null) {
            CartItem cartItem = cartItemRepository.findByCart_IdAndSkuId(cart.getId(), skuId)
                    .orElse(null);
            if (cartItem != null) {
                productId = cartItem.getProductId();
                // Remove from cart
                cart.removeItem(cartItem);
                cartItemRepository.delete(cartItem);
            }
        }

        if (productId == null) {
            throw new CartException(CartErrorCode.ITEM_NOT_FOUND);
        }

        // Check if already saved
        if (savedItemRepository.existsByUserIdAndSkuId(userId, skuId)) {
            throw new CartException(CartErrorCode.SAVED_ITEM_ALREADY_EXISTS);
        }

        SavedItem savedItem = SavedItem.builder()
                .userId(userId)
                .skuId(skuId)
                .productId(productId)
                .build();

        savedItem = savedItemRepository.save(savedItem);
        log.info("Saved item for later: userId={}, skuId={}", userId, skuId);

        return mapper.toSavedItemResponse(savedItem);
    }

    @Transactional
    public CartResponse moveToCart(UUID userId, UUID skuId, UUID sellerId) {
        SavedItem savedItem = savedItemRepository.findByUserIdAndSkuId(userId, skuId)
                .orElseThrow(() -> new CartException(CartErrorCode.SAVED_ITEM_NOT_FOUND));

        // Add to cart
        AddToCartRequest request = AddToCartRequest.builder()
                .skuId(savedItem.getSkuId())
                .productId(savedItem.getProductId())
                .sellerId(sellerId)
                .quantity(1)
                .build();

        CartResponse cart = addToCart(userId, request);

        // Remove from saved items
        savedItemRepository.delete(savedItem);
        log.info("Moved saved item to cart: userId={}, skuId={}", userId, skuId);

        return cart;
    }

    @Transactional(readOnly = true)
    public List<SavedItemResponse> getSavedItems(UUID userId) {
        List<SavedItem> items = savedItemRepository.findAllByUserId(userId);
        return mapper.toSavedItemResponseList(items);
    }

    @Transactional
    public void removeSavedItem(UUID userId, UUID skuId) {
        if (!savedItemRepository.existsByUserIdAndSkuId(userId, skuId)) {
            throw new CartException(CartErrorCode.SAVED_ITEM_NOT_FOUND);
        }
        savedItemRepository.deleteByUserIdAndSkuId(userId, skuId);
        log.info("Removed saved item: userId={}, skuId={}", userId, skuId);
    }

    // =====================================================
    // Cart Count (for header badge)
    // =====================================================

    @Transactional(readOnly = true)
    public int getCartItemCount(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null) {
            return 0;
        }
        return cartItemRepository.countByCartId(cart.getId());
    }

    // =====================================================
    // Internal Operations
    // =====================================================

    @Transactional(readOnly = true)
    public List<UUID> getCartSkuIds(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null) {
            return List.of();
        }
        return cartItemRepository.findSkuIdsByCartId(cart.getId());
    }

    @Transactional
    public void removeSelectedItems(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null) {
            return;
        }

        List<CartItem> selectedItems = cartItemRepository.findAllByCart_IdAndSelectedTrue(cart.getId());
        for (CartItem item : selectedItems) {
            cart.removeItem(item);
            cartItemRepository.delete(item);
        }

        log.info("Removed {} selected items from cart: userId={}", selectedItems.size(), userId);
    }

    // =====================================================
    // Helper Methods
    // =====================================================

    private Cart getOrCreateCart(UUID userId) {
        return cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .userId(userId)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    // =====================================================
    // Selected Items (for Checkout)
    // =====================================================

    /**
     * Get only the selected items from a user's cart (used by order-service during checkout).
     */
    @Transactional(readOnly = true)
    public List<CartItemResponse> getSelectedItems(UUID userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) {
            return List.of();
        }

        List<CartItem> selectedItems = cart.getItems().stream()
                .filter(CartItem::getSelected)
                .toList();

        if (selectedItems.isEmpty()) {
            throw new CartException(CartErrorCode.CART_EMPTY, "No items selected for checkout");
        }

        List<CartItemResponse> responses = mapper.toCartItemResponseList(selectedItems);
        enrichCartItems(responses);
        return responses;
    }

    /**
     * Get selected items grouped by seller (for checkout preview with shipping per seller).
     */
    @Transactional(readOnly = true)
    public List<CartBySellerResponse> getSelectedItemsGroupedBySeller(UUID userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) {
            return List.of();
        }

        Map<UUID, List<CartItem>> selectedBySeller = cart.getItems().stream()
                .filter(CartItem::getSelected)
                .collect(Collectors.groupingBy(CartItem::getSellerId));

        if (selectedBySeller.isEmpty()) {
            throw new CartException(CartErrorCode.CART_EMPTY, "No items selected for checkout");
        }

        // Fetch seller names in batch
        Map<UUID, String> sellerNameMap = fetchSellerNames(new ArrayList<>(selectedBySeller.keySet()));

        return selectedBySeller.entrySet().stream()
                .map(entry -> {
                    UUID sellerId = entry.getKey();
                    List<CartItem> items = entry.getValue();
                    List<CartItemResponse> itemResponses = mapper.toCartItemResponseList(items);
                    enrichCartItems(itemResponses);

                    BigDecimal subtotal = itemResponses.stream()
                            .map(CartItemResponse::getSubtotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return CartBySellerResponse.builder()
                            .sellerId(sellerId)
                            .sellerName(sellerNameMap.get(sellerId))
                            .items(itemResponses)
                            .subtotal(subtotal)
                            .itemCount(items.size())
                            .allSelected(true)
                            .build();
                })
                .toList();
    }

    // =====================================================
    // Direct Save to Wishlist (without being in cart)
    // =====================================================

    /**
     * Save an item directly to wishlist (from product page, not from cart).
     */
    @Transactional
    public SavedItemResponse saveItemDirectly(UUID userId, SaveForLaterRequest request) {
        // Check if already saved
        if (savedItemRepository.existsByUserIdAndSkuId(userId, request.getSkuId())) {
            throw new CartException(CartErrorCode.SAVED_ITEM_ALREADY_EXISTS);
        }

        SavedItem savedItem = SavedItem.builder()
                .userId(userId)
                .skuId(request.getSkuId())
                .productId(request.getProductId())
                .build();

        savedItem = savedItemRepository.save(savedItem);
        log.info("Directly saved item to wishlist: userId={}, skuId={}", userId, request.getSkuId());

        return mapper.toSavedItemResponse(savedItem);
    }

    // =====================================================
    // Saved Item Count (for badge)
    // =====================================================

    @Transactional(readOnly = true)
    public long getSavedItemCount(UUID userId) {
        return savedItemRepository.countByUserId(userId);
    }

    // =====================================================
    // Enrichment — fills product & inventory data on CartItemResponse
    // =====================================================

    private void enrichCartItems(List<CartItemResponse> items) {
        if (items == null || items.isEmpty()) return;

        // 1. Collect all SKU IDs
        List<UUID> skuIds = items.stream()
                .map(CartItemResponse::getSkuId)
                .distinct()
                .toList();

        // 2. Fetch SKU info from product-service
        Map<UUID, SkuInfo> skuInfoMap = fetchSkuInfoBatch(skuIds);

        // 3. Fetch inventory info from inventory-service
        Map<UUID, InventoryInfo> inventoryMap = fetchInventoryBatch(skuIds);

        // 4. Enrich each item
        for (CartItemResponse item : items) {
            SkuInfo sku = skuInfoMap.get(item.getSkuId());
            if (sku != null) {
                item.setProductName(sku.getProductName());
                item.setSkuCode(sku.getSkuCode());
                item.setImageUrl(sku.getImageUrl());
                item.setAttributes(sku.getAttributes());
                // Update price from product-service (cart stores price at add-time)
                if (sku.getPrice() != null) {
                    item.setPrice(sku.getPrice());
                    item.setSubtotal(sku.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }

            InventoryInfo inv = inventoryMap.get(item.getSkuId());
            if (inv != null) {
                item.setAvailableStock(inv.getAvailableStock());
                item.setInStock(Boolean.TRUE.equals(inv.getIsAvailable()));
            } else {
                // Fallback: use stock from product-service
                if (sku != null && sku.getStockQuantity() != null) {
                    item.setAvailableStock(sku.getStockQuantity());
                    item.setInStock(sku.getStockQuantity() > 0);
                } else {
                    item.setAvailableStock(0);
                    item.setInStock(false);
                }
            }
        }
    }

    private Map<UUID, SkuInfo> fetchSkuInfoBatch(List<UUID> skuIds) {
        try {
            ApiResponse<List<SkuInfo>> response = productClient.getBatchSkusByIds(skuIds);
            if (response != null && response.getData() != null) {
                return response.getData().stream()
                        .collect(Collectors.toMap(SkuInfo::getId, Function.identity(), (a, b) -> a));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch SKU info from product-service: {}", e.getMessage());
        }
        return Map.of();
    }

    private Map<UUID, InventoryInfo> fetchInventoryBatch(List<UUID> skuIds) {
        try {
            ApiResponse<List<InventoryInfo>> response = inventoryClient.checkAvailability(skuIds);
            if (response != null && response.getData() != null) {
                return response.getData().stream()
                        .collect(Collectors.toMap(InventoryInfo::getSkuId, Function.identity(), (a, b) -> a));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch inventory info from inventory-service: {}", e.getMessage());
        }
        return Map.of();
    }

    private Map<UUID, String> fetchSellerNames(List<UUID> sellerIds) {
        if (sellerIds == null || sellerIds.isEmpty()) return Map.of();
        try {
            ApiResponse<List<SellerInfo>> response = sellerClient.getBatchSellers(sellerIds);
            if (response != null && response.getData() != null) {
                return response.getData().stream()
                        .collect(Collectors.toMap(SellerInfo::getId, SellerInfo::getShopName, (a, b) -> a));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch seller names from seller-service: {}", e.getMessage());
        }
        return Map.of();
    }
}

