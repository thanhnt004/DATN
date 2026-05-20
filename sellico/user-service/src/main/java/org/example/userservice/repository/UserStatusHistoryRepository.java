package org.example.userservice.repository;

import org.example.userservice.entity.UserStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserStatusHistoryRepository
        extends JpaRepository<UserStatusHistory, UUID> {

    List<UserStatusHistory> findByUserIdOrderByChangedAtDesc(UUID userId);
}