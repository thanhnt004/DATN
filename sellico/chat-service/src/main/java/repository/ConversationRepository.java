package repository;

import entity.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {

    // Lấy tất cả conversation của user
    List<Conversation> findByMembersUserId(String userId);

    // Tìm private chat giữa 2 user
    Optional<Conversation> findByTypeAndMembersUserIdIn(
            String type,
            List<String> userIds
    );
    @Query("{ 'type': 'PRIVATE', 'members.userId': { $all: ?0 }, 'members': { $size: 2 } }")
    Optional<Conversation> findPrivateConversation(List<String> userIds);

    Optional<Conversation> findByConversationKey(String key);

    Optional<Conversation> findConversationById(String id);
}