package entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "message_status")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatus {

    @Id
    private String id;

    private String messageId;

    private String userId;

    private String status; // SENT | DELIVERED | SEEN

    private Instant updatedAt;
}