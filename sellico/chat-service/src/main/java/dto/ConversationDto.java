package dto;

import entity.Conversation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationDto {
    private String id;

    private String type; // PRIVATE | GROUP

    private String createdBy;

    private String conversationKey;

    private Instant createdAt;

    private List<Conversation.Member> members;

    private Long unreadCount;
}
