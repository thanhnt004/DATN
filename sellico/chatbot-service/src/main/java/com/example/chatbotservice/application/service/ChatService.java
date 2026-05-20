package com.example.chatbotservice.application.service;

import com.example.chatbotservice.adapter.in.web.request.ChatRequest;
import com.example.chatbotservice.adapter.in.web.response.ChatResponse;
import com.example.chatbotservice.application.port.in.ChatUseCase;
import com.example.chatbotservice.application.port.in.ClearChatHistoryUseCase;
import com.example.chatbotservice.application.port.out.ChatSessionStoragePort;
import com.example.chatbotservice.application.port.out.GeminiClientPort;
import com.example.chatbotservice.application.port.out.ProductClientPort;
import com.example.chatbotservice.domain.model.ChatMessage;
import com.example.chatbotservice.domain.model.ChatSession;
import com.example.chatbotservice.domain.model.ProductInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService implements ChatUseCase, ClearChatHistoryUseCase {

    private static final int MAX_HISTORY_MESSAGES = 20;
    private static final int MAX_PRODUCT_RESULTS = 6;

    private static final String SYSTEM_INSTRUCTION = """
            Bạn là trợ lý mua sắm thông minh của Sellico — một sàn thương mại điện tử.
            Nhiệm vụ của bạn là giúp khách hàng tìm kiếm và gợi ý sản phẩm phù hợp.
            
            QUY TẮC:
            1. Luôn trả lời bằng tiếng Việt, thân thiện và ngắn gọn.
            2. Khi khách hỏi về sản phẩm, hãy phân tích nhu cầu và gợi ý.
            3. Khi bạn muốn tìm sản phẩm, hãy trả về từ khóa tìm kiếm trong tag đặc biệt: [SEARCH:từ khóa tìm kiếm]
               Ví dụ: [SEARCH:áo thun nam] hoặc [SEARCH:laptop gaming]
               Bạn có thể đặt nhiều tag [SEARCH:...] nếu cần tìm nhiều loại sản phẩm.
            4. Ngoài tag tìm kiếm, hãy viết phần trả lời mô tả cho khách hàng.
            5. Không bịa thông tin sản phẩm — chỉ gợi ý dựa trên kết quả tìm kiếm thực tế.
            6. Nếu khách hỏi ngoài phạm vi mua sắm, hãy lịch sự hướng họ quay lại chủ đề sản phẩm.
            7. Bạn có thể hỏi thêm về ngân sách, sở thích, mục đích sử dụng để gợi ý chính xác hơn.
            
            VÍ DỤ:
            - Khách: "Tôi muốn mua điện thoại tầm 5 triệu"
              Bạn: "Để tìm điện thoại phù hợp với ngân sách 5 triệu, mình sẽ gợi ý một số lựa chọn nhé! [SEARCH:điện thoại giá rẻ]"
            - Khách: "Gợi ý quà sinh nhật cho bạn gái"
              Bạn: "Một số gợi ý quà tặng bạn gái phổ biến: [SEARCH:quà tặng bạn gái] [SEARCH:túi xách nữ]"
            """;

    private static final Pattern SEARCH_PATTERN = Pattern.compile("\\[SEARCH:([^\\]]+)]");

    private final GeminiClientPort geminiClient;
    private final ProductClientPort productClient;
    private final ChatSessionStoragePort sessionStorage;

    @Override
    public ChatResponse chat(ChatRequest request, String userId) {
        // 1. Get or create session
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        ChatSession session = sessionStorage.findBySessionId(sessionId)
                .orElse(ChatSession.builder()
                        .sessionId(sessionId)
                        .userId(userId)
                        .messages(new ArrayList<>())
                        .build());

        // 2. Call Gemini with conversation history
        String aiResponse;
        try {
            aiResponse = geminiClient.generateResponse(
                    SYSTEM_INSTRUCTION,
                    session.getMessages(),
                    request.getMessage()
            );
        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            aiResponse = "Xin lỗi, mình đang gặp sự cố kỹ thuật. Bạn vui lòng thử lại sau nhé!";
        }

        // 3. Extract search keywords and fetch products
        List<String> searchKeywords = extractSearchKeywords(aiResponse);
        List<ProductInfo> suggestedProducts = new ArrayList<>();

        for (String keyword : searchKeywords) {
            try {
                List<ProductInfo> products = productClient.searchProducts(keyword.trim(), 0, MAX_PRODUCT_RESULTS);
                suggestedProducts.addAll(products);
            } catch (Exception e) {
                log.warn("Error searching products for keyword '{}': {}", keyword, e.getMessage());
            }
        }

        // Remove duplicates by product ID
        suggestedProducts = suggestedProducts.stream()
                .distinct()
                .limit(MAX_PRODUCT_RESULTS)
                .toList();

        // 4. Clean the AI response (remove [SEARCH:...] tags)
        String cleanReply = cleanResponse(aiResponse);

        // 5. Save messages to session
        session.addMessage(ChatMessage.builder()
                .role("user")
                .content(request.getMessage())
                .timestamp(Instant.now())
                .build());

        session.addMessage(ChatMessage.builder()
                .role("model")
                .content(cleanReply)
                .timestamp(Instant.now())
                .build());

        session.trimHistory(MAX_HISTORY_MESSAGES);
        sessionStorage.save(session);

        // 6. Build response
        return ChatResponse.builder()
                .sessionId(sessionId)
                .reply(cleanReply)
                .suggestedProducts(suggestedProducts.isEmpty() ? null : suggestedProducts)
                .build();
    }

    @Override
    public void clearHistory(String sessionId, String userId) {
        sessionStorage.deleteBySessionId(sessionId);
        log.info("Cleared chat history for session: {}, user: {}", sessionId, userId);
    }

    private List<String> extractSearchKeywords(String response) {
        List<String> keywords = new ArrayList<>();
        Matcher matcher = SEARCH_PATTERN.matcher(response);
        while (matcher.find()) {
            keywords.add(matcher.group(1).trim());
        }
        return keywords;
    }

    private String cleanResponse(String response) {
        return SEARCH_PATTERN.matcher(response).replaceAll("").trim();
    }
}
