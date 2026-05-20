package event;

import java.time.Instant;

public record EventMetadata(
        String eventId,
        String eventType,
        String source,
        Instant occurredAt
) {}