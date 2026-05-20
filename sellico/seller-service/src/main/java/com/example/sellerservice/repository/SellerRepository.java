package com.example.sellerservice.repository;

import com.example.sellerservice.entity.Seller;
import com.example.sellerservice.entity.enums.SellerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerRepository extends JpaRepository<Seller, UUID> {

    Optional<Seller> findByUserId(UUID userId);

    Optional<Seller> findByShopSlug(String shopSlug);

    boolean existsByUserId(UUID userId);

    boolean existsByShopSlug(String shopSlug);

    boolean existsByShopName(String shopName);

    List<Seller> findAllByStatus(SellerStatus status);

    Page<Seller> findAllByStatus(SellerStatus status, Pageable pageable);

    @Query("SELECT s FROM Seller s WHERE s.status = :status ORDER BY s.ratingAvg DESC")
    List<Seller> findTopRatedSellers(@Param("status") SellerStatus status, Pageable pageable);

    @Query("SELECT s FROM Seller s WHERE s.status = 'ACTIVE' AND " +
           "(LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Seller> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM Seller s WHERE s.status = 'ACTIVE' AND s.city = :city")
    Page<Seller> findByCity(@Param("city") String city, Pageable pageable);

    @Query("SELECT s FROM Seller s WHERE s.id IN :ids")
    List<Seller> findAllByIds(@Param("ids") List<UUID> ids);

    @Query("SELECT COUNT(s) FROM Seller s WHERE s.status = :status")
    long countByStatus(@Param("status") SellerStatus status);

    @Query("SELECT s FROM Seller s WHERE " +
           "(:keyword IS NULL OR LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY s.createdAt DESC")
    Page<Seller> searchForAdmin(@Param("keyword") String keyword, Pageable pageable);
}

