package repository;

import entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends MongoRepository<Message, String> {

    // Load message theo conversation (pagination)
    Page<Message> findByConversationIdOrderByCreatedAtDesc(
            String conversationId,
            Pageable pageable
    );

    // Load message cũ hơn (infinite scroll)
    List<Message> findByConversationIdAndCreatedAtLessThanOrderByCreatedAtDesc(
            String conversationId,
            Instant before,
            Pageable pageable
    );

    // Đếm message (ít dùng nhưng có thể cần)
    long countByConversationId(String conversationId);

    Optional<Message> findByIdAndConversationId(String id, String conversationId);
}
