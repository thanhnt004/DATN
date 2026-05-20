package controller;

import dto.ConversationCreateRequest;
import dto.ConversationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.ConversationService;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final service.ChatService chatService;

    // Lấy danh sách hội thoại của tôi (Lấy UserId từ Token/Session)
    @GetMapping
    public ResponseEntity<List<ConversationDto>> getMyConversations(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(conversationService.getUserConversations(userId));
    }

    // Resolve or create private conversation with specific user
    @GetMapping("/resolve")
    public ResponseEntity<ConversationDto> resolvePrivateConversation(
            @RequestParam String userId,
            @RequestParam(required = false) String userRole,
            @RequestHeader("X-User-Id") String currentUserId,
            @RequestHeader(value = "X-User-Role", required = false) String currentUserRole) {
        var conversation = conversationService.resolveOrCreatePrivateConversation(currentUserId, currentUserRole, userId, userRole);
        return ResponseEntity.ok(conversationService.toConversationDto(conversation,currentUserId));
    }

    // Tạo phòng chat mới
    @PostMapping
    public ResponseEntity<ConversationDto> create(@RequestBody ConversationCreateRequest request,
                                                     @RequestHeader("X-User-Id") String creatorId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conversationService.createConversation(request, creatorId));
    }

    // Rời nhóm
    @DeleteMapping("/{id}/leave")
    public ResponseEntity<Void> leave(@PathVariable String id, @RequestHeader("X-User-Id") String userId) {
        conversationService.leaveConversation(id, userId);
        return ResponseEntity.noContent().build();
    }
}
