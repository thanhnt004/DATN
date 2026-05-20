package service;

import dto.Action;
import dto.Feature;
import dto.SendMessageRequest;
import dto.WsMessage;
import dto.response.ChatMessageResponse;
import entity.Conversation;
import entity.LastMessage;
import entity.Message;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import repository.ConversationRepository;
import repository.MessageRepository;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Logger log = LogManager.getLogger(ChatService.class);
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationService conversationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ProcessedMessage processIncomingMessage(WsMessage<SendMessageRequest> wsMessage) {
        String senderId = wsMessage.getFrom();

        Conversation conversation;

        if (wsMessage.getTo() == null) {

            String type = wsMessage.getPayload().getConversationType();

            if ("PRIVATE".equals(type)) {

                List<String> mIds = wsMessage.getPayload().getMemberIds();
                String receiverId = mIds.stream().filter(id -> !id.equals(senderId)).findFirst().orElse(senderId);
                conversation = conversationService.resolveOrCreatePrivateConversation(
                        senderId, null, receiverId, null
                );

            } else if ("GROUP".equals(type)) {

                conversation = conversationService.createGroupConversation(
                        senderId,
                        wsMessage.getPayload().getMemberIds()
                );

            } else {
                throw new RuntimeException("Invalid conversation type");
            }

        } else {
            String toValue = wsMessage.getTo();
            Optional<Conversation> existingConversation = conversationRepository.findById(toValue);
            if (existingConversation.isPresent()) {
                conversation = existingConversation.get();
            } else {
                String type = wsMessage.getPayload().getConversationType();
                if (!"PRIVATE".equals(type)) {
                    throw new RuntimeException("Conversation not found: " + toValue);
                }
                List<String> memberIds = new ArrayList<>(
                        Optional.ofNullable(wsMessage.getPayload().getMemberIds()).orElseGet(ArrayList::new)
                );
                if (!memberIds.contains(toValue)) {
                    memberIds.add(toValue);
                }
                String receiverId = memberIds.stream().filter(id -> !id.equals(senderId)).findFirst().orElse(senderId);
                conversation = conversationService.resolveOrCreatePrivateConversation(senderId, null, receiverId, null);
            }
        }

        String conversationId = conversation.getId();

        boolean isMember = conversation.getMembers().stream()
                .anyMatch(m -> m.getUserId().equals(senderId));

        if (!isMember) {
            throw new RuntimeException("User not in conversation");
        }

        // 2. build message
        Message message = Message.builder()
                .id(generateId())
                .conversationId(conversationId)
                .senderId(senderId)
                .content(wsMessage.getPayload().getContent())
                .type(wsMessage.getPayload().getType())
                .attachments(mapAttachments(wsMessage))
                .createdAt(Instant.now())
                .deleted(false)
                .build();

        // 3. save DB
        messageRepository.save(message);

        conversation.setLastMessage(
                LastMessage.builder()
                        .messageId(message.getId())
                        .senderId(senderId)
                        .content(message.getContent())
                        .createdAt(message.getCreatedAt())
                        .build()
        );
        conversationRepository.save(conversation);
        log.info("Conversation id {} saved?????????????!!!!!", conversationId);
        WsMessage<ChatMessageResponse> outgoingMessage = buildOutgoingMessage(message);
        List<String> recipientUserIds = conversation.getMembers()
                .stream()
                .map(Conversation.Member::getUserId)
                .toList();
        return new ProcessedMessage(outgoingMessage, recipientUserIds);
    }

    public Page<ChatMessageResponse> getConversationHistory(
            String conversationId,
            String currentUserId,
            int page,
            int size,
            Instant before
    ) {
        Conversation conversation = getConversationAndValidateMember(conversationId, currentUserId);
        if (conversation == null) {
            throw new RuntimeException("Conversation not found");
        }

        if (before != null) {
            List<Message> items = messageRepository.findByConversationIdAndCreatedAtLessThanOrderByCreatedAtDesc(
                    conversationId,
                    before,
                    PageRequest.of(0, size)
            );
            List<ChatMessageResponse> mapped = items.stream().map(this::toResponse).toList();
            return new PageImpl<>(mapped, PageRequest.of(0, size), mapped.size());
        }

        Page<Message> messagePage = messageRepository.findByConversationIdOrderByCreatedAtDesc(
                conversationId,
                PageRequest.of(page, size)
        );
        return messagePage.map(this::toResponse);
    }

    public ChatMessageResponse deleteMessage(
            String conversationId,
            String messageId,
            String currentUserId
    ) {
        getConversationAndValidateMember(conversationId, currentUserId);
        Message message = messageRepository.findByIdAndConversationId(messageId, conversationId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!currentUserId.equals(message.getSenderId())) {
            throw new RuntimeException("No permission to delete message");
        }

        message.setDeleted(true);
        message.setContent("");
        message.setEditedAt(Instant.now());
        messageRepository.save(message);

        return toResponse(message);
    }

    private Conversation getConversationAndValidateMember(String conversationId, String currentUserId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        boolean isMember = conversation.getMembers().stream()
                .anyMatch(m -> m.getUserId().equals(currentUserId));
        if (!isMember) {
            throw new RuntimeException("User not in conversation");
        }
        return conversation;
    }

    private List<Message.Attachment> mapAttachments(WsMessage<SendMessageRequest> wsMessage) {
        List<Message.Attachment> attachments = new ArrayList<>();
        wsMessage.getPayload().getAttachments().forEach(attachment -> {
            attachments.add(new Message.Attachment(attachment.getUrl(), attachment.getType(), attachment.getSize()));
        });
        return attachments;
    }

    private ChatMessageResponse toResponse(Message message) {
        return ChatMessageResponse.builder()
                .messageId(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .type(message.getType())
                .createdAt(message.getCreatedAt() != null ? message.getCreatedAt().toEpochMilli() : 0L)
                .editedAt(message.getEditedAt() != null ? message.getEditedAt().toEpochMilli() : null)
                .deleted(message.isDeleted())
                .build();
    }

    // ===== build message trả về client =====
    private WsMessage<ChatMessageResponse> buildOutgoingMessage(Message message) {

        ChatMessageResponse payload = ChatMessageResponse.builder()
                .messageId(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .createdAt(message.getCreatedAt().toEpochMilli())
                .build();

        WsMessage<ChatMessageResponse> ws = new WsMessage<>();
        ws.setTraceId(UUID.randomUUID().toString());
        ws.setFeature(Feature.CHAT.toString());
        ws.setAction(Action.SEND.toString());

        ws.setFrom(message.getSenderId());
        ws.setTo(message.getConversationId());
        ws.setTimestamp(System.currentTimeMillis());

        ws.setPayload(payload);

        return ws;
    }
    private String generateId() {
        return UUID.randomUUID().toString();
    }

    public record ProcessedMessage(
            WsMessage<ChatMessageResponse> outgoingMessage,
            List<String> recipientUserIds
    ) {}
}
