package org.example.notificationservice.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.example.notificationservice.domain.model.ChannelType;
import org.example.notificationservice.domain.model.Provider;
import org.example.notificationservice.domain.port.out.ProviderRepository;
import org.example.notificationservice.infrastructure.persistence.JpaProviderRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProviderRepositoryAdapter implements ProviderRepository {

    private final JpaProviderRepository jpaRepository;

    @Override
    public Optional<Provider> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Provider> findByChannelTypeAndIsActiveOrderByPriority(ChannelType channelType, boolean isActive) {
        return jpaRepository.findByChannelTypeAndIsActiveOrderByPriorityAsc(channelType, isActive);
    }

    @Override
    public Provider save(Provider provider) {
        return jpaRepository.save(provider);
    }
}

