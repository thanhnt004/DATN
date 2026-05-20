package org.example.notificationservice.infrastructure.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.example.notificationservice.domain.model.NotificationStatus;

@Converter(autoApply = false)
public class NotificationStatusConverter implements AttributeConverter<NotificationStatus, String> {

    @Override
    public String convertToDatabaseColumn(NotificationStatus status) {
        return status == null ? null : status.name();
    }

    @Override
    public NotificationStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : NotificationStatus.valueOf(dbData);
    }
}

