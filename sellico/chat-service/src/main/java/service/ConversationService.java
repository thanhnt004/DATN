package service;

import dto.ConversationCreateRequest;
import dto.ConversationDto;
import entity.Conversation;
import lombok.RequiredArgsConstructor;
import mapper.ConversationMapper;
import org.springframework.stereotype.Service;
import repository.ConversationRepository;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationService {
    private final ConversationMapper conversationMapper;
    private final ConversationRepository conversationRepository;
    public List<ConversationDto> getUserConversations(String userId) {

        return conversationRepository.findByMembersUserId(userId).stream()
                .map(cvs->{return this.toConversationDto(cvs,userId);
                }).collect(Collectors.toList());
    }

    public Conversation createGroupConversation(String senderId, List<String> memberIds) {
        List<String> mutableMemberIds = new ArrayList<>(memberIds);

        // Đảm bảo có senderId
        if (!mutableMemberIds.contains(senderId)) {
            mutableMemberIds.add(senderId);
        }
        if (!mutableMemberIds.contains(senderId)) {
            mutableMemberIds.add(senderId);
        }

        // group luôn tạo mới → KHÔNG check duplicate
        return conversationRepository.save(
                Conversation.builder()
                        .id(generateId())
                        .type("GROUP")
                        .createdBy(senderId)
                        .createdAt(Instant.now())
                        .members(
                                mutableMemberIds.stream()
                                        .distinct()
                                        .map(u -> {
                                            if (u.equals(senderId)) {
                                                return Conversation.Member.builder()
                                                        .userId(u)
                                                        .role("ADMIN")
                                                        .joinedAt(Instant.now())
                                                        .build();
                                            }
                                            return Conversation.Member.builder()
                                                    .userId(u)
                                                    .role("MEMBER")
                                                    .joinedAt(Instant.now())
                                                    .build();
                                                }
                                        )
                                        .toList()
                        )
                        .build()
        );
    }
    public Conversation resolveOrCreatePrivateConversation(String senderId, String senderRole, String receiverId, String receiverRole) {
        List<String> mutableMemberIds = new ArrayList<>(Arrays.asList(senderId, receiverId));
        mutableMemberIds = mutableMemberIds.stream().distinct().collect(Collectors.toList());

        // 2. chỉ cho private = 2 người
        if (mutableMemberIds.size() != 2) {
            throw new RuntimeException("Invalid private chat");
        }

        String key = buildConversationKey(mutableMemberIds);

        // 3. tìm trước
        Optional<Conversation> existing =
                conversationRepository.findByConversationKey(key);

        if (existing.isPresent()) {
            return existing.get();
        }

        // 4. tạo mới (có thể bị race condition)
        Conversation newConv = Conversation.builder()
                .id(generateId())
                .type("PRIVATE")
                .createdBy(senderId)
                .conversationKey(key)
                .createdAt(Instant.now())
                .members(
                        Arrays.asList(
                                Conversation.Member.builder()
                                        .userId(senderId)
                                        .role("MEMBER")
                                        .userRole(senderRole != null ? senderRole : "USER")
                                        .joinedAt(Instant.now())
                                        .build(),
                                Conversation.Member.builder()
                                        .userId(receiverId)
                                        .role("MEMBER")
                                        .userRole(receiverRole != null ? receiverRole : "USER")
                                        .joinedAt(Instant.now())
                                        .build()
                        )
                )
                .build();

        try {
            return conversationRepository.save(newConv);
        } catch (Exception e) {
            // 🔥 nếu bị duplicate key → thằng khác đã tạo trước
            return conversationRepository.findByConversationKey(key)
                    .orElseThrow(() -> new RuntimeException("Race condition failed"));
        }
    }
    private String buildConversationKey(List<String> userIds) {

        List<String> sorted = new ArrayList<>(userIds);
        Collections.sort(sorted);

        return String.join("_", sorted);
    }
    private String generateId() {
        return UUID.randomUUID().toString();
    }
    public void leaveConversation(String conversationId, String userId) {
        Optional<Conversation> existing =
                conversationRepository.findConversationById(conversationId);

        if (existing.isEmpty()) {
            throw new RuntimeException("NOT FOUND");
        }
        Conversation conversation = existing.get();
        conversation.getMembers().stream().filter(u -> u.getUserId().equals(userId)).findFirst().ifPresent(u -> {conversation.getMembers().remove(u);});
        conversationRepository.save(conversation);
    }
    private void addParticipant(String convId, String userId) {
        Conversation.Member member = new Conversation.Member();
        member.setUserId(userId);
        member.setRole("MEMBER");
        member.setJoinedAt(Instant.now());
        Optional<Conversation> existing =
                conversationRepository.findConversationById(convId);

        if (existing.isEmpty()) {
            throw new RuntimeException("NOT FOUND");
        }
        Conversation conversation = existing.get();
        conversation.getMembers().add(member);
        conversationRepository.save(conversation);
    }

    public ConversationDto toConversationDto(entity.Conversation conversation,String userId) {
        return conversationMapper.toDto(conversation, userId);
    }

    public ConversationDto createConversation(ConversationCreateRequest request, String creatorId) {
        if (request.getType().equals("PRIVATE")) {
            String receiverId = request.getUserIds().stream().filter(id -> !id.equals(creatorId)).findFirst().orElse(creatorId);
            return toConversationDto(resolveOrCreatePrivateConversation(creatorId, null, receiverId, null), creatorId);
        }

        return toConversationDto(createGroupConversation(creatorId, request.getUserIds()), creatorId);
    }
}
