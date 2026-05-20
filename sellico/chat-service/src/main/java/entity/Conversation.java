package entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@CompoundIndex(name = "member_user_idx", def = "{'members.userId': 1}")
@CompoundIndex(name = "uniq_conv_key", def = "{'conversationKey': 1}", unique = true)
@Document(collection = "conversations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    private String id;

    private String type; // PRIVATE | GROUP

    private String createdBy;

    private String conversationKey;

    private String name;

    private String avatarUrl;

    private Instant createdAt;

    private List<Member> members;

    // cache để load list chat nhanh
    private LastMessage lastMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Member {

        private String userId;

        private long unreadCount;

        private String role; // MEMBER | ADMIN

        private String userRole; // BUYER | SELLER

        private Instant joinedAt;

        private String lastReadMessageId;
    }
}
