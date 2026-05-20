package entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@CompoundIndexes({
        @CompoundIndex(name = "conv_time_idx", def = "{'conversationId': 1, 'createdAt': -1}")
})
@Document(collection = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    private String id;

    private String conversationId;

    private String senderId;

    private String content;

    private String type; // TEXT | IMAGE | FILE

    private List<Attachment> attachments;

    private Instant createdAt;

    private Instant editedAt;

    private boolean deleted;
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attachment {

        private String url;

        private String type;

        private long size;
    }
}
