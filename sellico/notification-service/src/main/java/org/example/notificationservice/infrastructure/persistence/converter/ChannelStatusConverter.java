package org.example.notificationservice.infrastructure.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.example.notificationservice.domain.model.ChannelStatus;

@Converter(autoApply = false)
public class ChannelStatusConverter implements AttributeConverter<ChannelStatus, String> {

    @Override
    public String convertToDatabaseColumn(ChannelStatus status) {
        return status == null ? null : status.name();
    }

    @Override
    public ChannelStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ChannelStatus.valueOf(dbData);
    }
}

