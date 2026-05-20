package com.example.cartservice.repository;

import com.example.cartservice.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findAllByCart_Id(UUID cartId);

    List<CartItem> findAllByCart_IdAndSelectedTrue(UUID cartId);

    Optional<CartItem> findByCart_IdAndSkuId(UUID cartId, UUID skuId);

    boolean existsByCart_IdAndSkuId(UUID cartId, UUID skuId);

    List<CartItem> findAllByCart_IdAndSellerId(UUID cartId, UUID sellerId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.skuId IN :skuIds")
    void deleteByCartIdAndSkuIds(@Param("cartId") UUID cartId, @Param("skuIds") List<UUID> skuIds);

    @Modifying
    @Query("UPDATE CartItem ci SET ci.selected = :selected WHERE ci.cart.id = :cartId")
    void updateAllSelection(@Param("cartId") UUID cartId, @Param("selected") boolean selected);

    @Modifying
    @Query("UPDATE CartItem ci SET ci.selected = :selected WHERE ci.cart.id = :cartId AND ci.sellerId = :sellerId")
    void updateSelectionBySeller(@Param("cartId") UUID cartId, @Param("sellerId") UUID sellerId, @Param("selected") boolean selected);

    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.cart.id = :cartId")
    int countByCartId(@Param("cartId") UUID cartId);

    @Query("SELECT ci.skuId FROM CartItem ci WHERE ci.cart.id = :cartId")
    List<UUID> findSkuIdsByCartId(@Param("cartId") UUID cartId);
}

