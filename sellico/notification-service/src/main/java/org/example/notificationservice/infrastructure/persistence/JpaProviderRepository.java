package org.example.notificationservice.infrastructure.persistence;

import org.example.notificationservice.domain.model.ChannelType;
import org.example.notificationservice.domain.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaProviderRepository extends JpaRepository<Provider, UUID> {

    List<Provider> findByChannelTypeAndIsActiveOrderByPriorityAsc(ChannelType channelType, boolean isActive);
}

