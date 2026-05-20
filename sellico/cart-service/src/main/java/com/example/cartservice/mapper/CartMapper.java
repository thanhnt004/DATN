package com.example.cartservice.mapper;

import com.example.cartservice.dto.response.CartItemResponse;
import com.example.cartservice.dto.response.CartResponse;
import com.example.cartservice.dto.response.SavedItemResponse;
import com.example.cartservice.entity.Cart;
import com.example.cartservice.entity.CartItem;
import com.example.cartservice.entity.SavedItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "subtotal", expression = "java(item.getSubtotal())")
    @Mapping(target = "productName", ignore = true)
    @Mapping(target = "skuCode", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "sellerName", ignore = true)
    @Mapping(target = "availableStock", ignore = true)
    @Mapping(target = "inStock", ignore = true)
    @Mapping(target = "attributes", ignore = true)
    CartItemResponse toCartItemResponse(CartItem item);

    List<CartItemResponse> toCartItemResponseList(List<CartItem> items);

    @Mapping(target = "productName", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "price", ignore = true)
    SavedItemResponse toSavedItemResponse(SavedItem item);

    List<SavedItemResponse> toSavedItemResponseList(List<SavedItem> items);

    default CartResponse toCartResponse(Cart cart) {
        if (cart == null) {
            return null;
        }

        List<CartItemResponse> items = toCartItemResponseList(cart.getItems());

        // Calculate summary
        int totalItems = items.size();
        int totalQuantity = items.stream().mapToInt(CartItemResponse::getQuantity).sum();
        int selectedItems = (int) items.stream().filter(CartItemResponse::getSelected).count();
        int selectedQuantity = items.stream()
                .filter(CartItemResponse::getSelected)
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal selectedSubtotal = items.stream()
                .filter(CartItemResponse::getSelected)
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CartResponse.CartSummary summary = CartResponse.CartSummary.builder()
                .totalItems(totalItems)
                .totalQuantity(totalQuantity)
                .selectedItems(selectedItems)
                .selectedQuantity(selectedQuantity)
                .subtotal(subtotal)
                .selectedSubtotal(selectedSubtotal)
                .build();

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(items)
                .summary(summary)
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
}

