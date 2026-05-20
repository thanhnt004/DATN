package controller;

import dto.response.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import service.ChatService;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/conversations/{conversationId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<Page<ChatMessageResponse>> getHistory(
            @PathVariable String conversationId,
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long before
    ) {
        Instant beforeInstant = before != null ? Instant.ofEpochMilli(before) : null;
        Page<ChatMessageResponse> result = chatService.getConversationHistory(
                conversationId,
                userId,
                page,
                size,
                beforeInstant
        );
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<ChatMessageResponse> deleteMessage(
            @PathVariable String conversationId,
            @PathVariable String messageId,
            @RequestHeader("X-User-Id") String userId
    ) {
        ChatMessageResponse result = chatService.deleteMessage(conversationId, messageId, userId);
        return ResponseEntity.ok(result);
    }
}
