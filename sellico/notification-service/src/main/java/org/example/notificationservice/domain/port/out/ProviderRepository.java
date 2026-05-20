package org.example.notificationservice.domain.port.out;

import org.example.notificationservice.domain.model.ChannelType;
import org.example.notificationservice.domain.model.Provider;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderRepository {

    Optional<Provider> findById(UUID id);

    List<Provider> findByChannelTypeAndIsActiveOrderByPriority(ChannelType channelType, boolean isActive);

    Provider save(Provider provider);
}

