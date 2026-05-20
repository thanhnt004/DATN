package repository;

import entity.MessageStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MessageStatusRepository extends MongoRepository<MessageStatus, String> {

    List<MessageStatus> findByMessageId(String messageId);

    List<MessageStatus> findByUserIdAndStatus(String userId, String status);
}
