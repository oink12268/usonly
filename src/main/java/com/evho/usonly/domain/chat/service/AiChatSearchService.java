package com.evho.usonly.domain.chat.service;

import com.evho.usonly.domain.chat.model.Chat;
import com.evho.usonly.domain.chat.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiChatSearchService {

    private final ChatRepository chatRepository;

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    @SuppressWarnings("unchecked")
    public String search(String query) {
        List<Chat> chats = chatRepository.findAllByOrderByCreatedAtAsc();

        String chatHistory = chats.stream()
                .filter(c -> c.getMessage() != null && !c.getMessage().startsWith("IMAGE:"))
                .map(c -> String.format("[%s] %s: %s",
                        c.getCreatedAt().toLocalDate(),
                        c.getWriterUid().substring(0, Math.min(6, c.getWriterUid().length())),
                        c.getMessage()))
                .collect(Collectors.joining("\n"));

        if (chatHistory.isBlank()) {
            return "채팅 내역이 없습니다.";
        }

        String prompt = String.format(
                "다음은 커플 채팅 내역이야. 아래 질문에 맞는 내용을 찾아서 한국어로 간결하게 정리해줘.\n" +
                "질문: %s\n\n" +
                "채팅 내역:\n%s\n\n" +
                "관련 내용이 없으면 '관련 대화를 찾지 못했습니다.'라고 답해줘. " +
                "날짜와 함께 정리해줘.",
                query, chatHistory
        );

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                GEMINI_URL + geminiApiKey, request, Map.class
        );

        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) response.getBody().get("candidates");
        Map<String, Object> content =
                (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts =
                (List<Map<String, Object>>) content.get("parts");

        return (String) parts.get(0).get("text");
    }
}
