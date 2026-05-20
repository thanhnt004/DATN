package dto.response;

import dto.Attachment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageResponse {

    private String messageId;

    private String conversationId;

    private String senderId;

    private String content;

    private String type; // TEXT | IMAGE | FILE

    private long createdAt;

    private Long editedAt; // nullable

    private boolean deleted;

    private List<Attachment> attachments;
}
