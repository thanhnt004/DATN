package kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dto.SendMessageRequest;
import dto.WsMessage;
import dto.response.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import service.ChatService;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatConsumerService {
    private final ChatService chatService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    @Value("${app.kafka.topic-out:ws.outbound}")
    private String topicOut;

    @KafkaListener(topics = "${app.kafka.topic-in}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume( @Payload WsMessage<Object> message,
                         Acknowledgment ack) {
        log.info("Processing message: {}", message.getTraceId());

        try {
            SendMessageRequest request = objectMapper.convertValue(
                    message.getPayload(),
                    SendMessageRequest.class
            );
            // 2. Build lại message đúng kiểu
            WsMessage<SendMessageRequest> typedMessage = new WsMessage<>();
            typedMessage.setTraceId(message.getTraceId());
            typedMessage.setFeature(message.getFeature());
            typedMessage.setAction(message.getAction());
            typedMessage.setFrom(message.getFrom());
            typedMessage.setTo(message.getTo());
            typedMessage.setTimestamp(message.getTimestamp());
            typedMessage.setPayload(request);
            ChatService.ProcessedMessage processed = chatService.processIncomingMessage(typedMessage);
            processed.recipientUserIds().forEach((userId) -> {
                WsMessage<ChatMessageResponse> outgoingForUser = new WsMessage<>();
                outgoingForUser.setTraceId(processed.outgoingMessage().getTraceId());
                outgoingForUser.setFeature(processed.outgoingMessage().getFeature());
                outgoingForUser.setAction(processed.outgoingMessage().getAction());
                outgoingForUser.setFrom(processed.outgoingMessage().getFrom());
                // IMPORTANT: websocket-worker routes by recipient user id.
                outgoingForUser.setTo(userId);
                outgoingForUser.setTimestamp(processed.outgoingMessage().getTimestamp());
                outgoingForUser.setPayload(processed.outgoingMessage().getPayload());
                kafkaTemplate.send(topicOut, userId, outgoingForUser);
                log.info("Published outbound chat traceId={} to userId={} via topic={}",
                        outgoingForUser.getTraceId(), userId, topicOut);
            });
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process message {}", message.getTraceId(), e);
            // Push to Retry/DLQ logic
        }
    }
}
