package org.example.userservice.repository;

import org.example.userservice.entity.User;
import org.example.userservice.entity.UserStatus;
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
public interface UserRepository extends JpaRepository<User, UUID> {
    @Query(
        "SELECT u.username FROM User u WHERE u.email = :identifier OR u.phone = :identifier OR u.username = :identifier"
    )
    String findUsernameByIdentifier(@Param("identifier") String identifier);

    Optional<User> findByAuthId(UUID authId);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, UUID id);

    boolean existsByEmailAndIdNot(String email, UUID id);

    List<User> findAllByIdIn(List<UUID> ids);

    @Query("""
        SELECT u FROM User u LEFT JOIN u.profile p
        WHERE (:keyword IS NULL OR u.username LIKE %:keyword% OR u.email LIKE %:keyword% 
               OR u.phone LIKE %:keyword% OR p.fullName LIKE %:keyword%)
        AND (:status IS NULL OR u.status = :status)
        AND u.deletedAt IS NULL
        ORDER BY u.createdAt DESC
    """)
    Page<User> searchUsers(@Param("keyword") String keyword,
                           @Param("status") UserStatus status,
                           Pageable pageable);
}
