package org.example.notificationservice.infrastructure.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.example.notificationservice.domain.model.ChannelType;

@Converter(autoApply = false)
public class ChannelTypeConverter implements AttributeConverter<ChannelType, String> {

    @Override
    public String convertToDatabaseColumn(ChannelType channelType) {
        return channelType == null ? null : channelType.name();
    }

    @Override
    public ChannelType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ChannelType.valueOf(dbData);
    }
}

