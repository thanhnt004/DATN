package mapper;

import dto.ConversationDto;
import entity.Conversation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ConversationMapper
{
    @Mapping(target = "unreadCount", ignore = true)
    ConversationDto toConversationDto(Conversation conversation);
    List<ConversationDto> toConversationDtos(List<Conversation> conversation);
    default ConversationDto toDto(Conversation entity, String userId) {
        if (entity == null) return null;

        ConversationDto dto = toConversationDto(entity);

        Long unread = entity.getMembers() == null ? 0L :
                entity.getMembers().stream()
                        .filter(m -> userId.equals(m.getUserId()))
                        .map(Conversation.Member::getUnreadCount)
                        .findFirst()
                        .orElse(0L);

        dto.setUnreadCount(unread);

        return dto;
    }

    default List<ConversationDto> toDtos(List<Conversation> entities, String userId) {
        if (entities == null) return List.of();

        return entities.stream()
                .map(e -> toDto(e, userId))
                .toList();
    }
}
