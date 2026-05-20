package com.example.chatbotservice.application.port.out;

import com.example.chatbotservice.domain.model.ChatMessage;

import java.util.List;

/**
 * Port for calling the Gemini AI API.
 */
public interface GeminiClientPort {

    /**
     * Send a message to Gemini with conversation history and system instruction.
     *
     * @param systemInstruction the system instruction/prompt
     * @param conversationHistory previous messages in the conversation
     * @param userMessage the current user message
     * @return the AI-generated response text
     */
    String generateResponse(String systemInstruction, List<ChatMessage> conversationHistory, String userMessage);
}
