package repository;

import entity.ChatUser;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatUserRepository extends MongoRepository<ChatUser, String> {

    List<ChatUser> findByIdIn(List<String> ids);
}