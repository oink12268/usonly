package com.evho.usonly.domain.chat.service;

import com.evho.usonly.domain.chat.entity.Chat;
import com.evho.usonly.domain.chat.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiChatSearchService {

    private final ChatRepository chatRepository;

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

    @SuppressWarnings("unchecked")
    public String search(String query) {
        List<Chat> chats = chatRepository.findAllByOrderByCreatedAtAsc();

        String chatHistory = chats.stream()
                .filter(c -> c.getMessage() != null && !c.getMessage().startsWith("IMAGE:"))
                .map(c -> String.format("[%s] %s: %s",
                        c.getCreatedAt(),
                        c.getWriterUid().substring(0, Math.min(6, c.getWriterUid().length())),
                        c.getMessage()))
                .collect(Collectors.joining("\n"));

        if (chatHistory.isBlank()) {
            return "채팅 내역이 없습니다.";
        }

        String prompt = String.format(
                "다음은 커플 채팅 내역이야. 질문에 맞는 내용을 찾아서 한국어로 간결하게 정리해줘.\n" +
                        "질문: %s\n\n" +
                        "채팅 내역:\n%s\n\n" +
                        "관련 내용이 없으면 '관련 대화를 찾지 못했습니다.'라고 답해줘.",
                query, chatHistory
        );

        try {
            RestTemplate restTemplate = new RestTemplate();

            // 1. 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", geminiApiKey);

            // 2. 바디 구성
            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    )
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            // 3. [가장 중요] URI 객체를 생성할 때 빌더를 사용하여 인코딩 문제를 원천 차단합니다.
            URI uri = UriComponentsBuilder.fromHttpUrl(GEMINI_API_URL)
                    .build()
                    .toUri();

            ResponseEntity<Map> response = restTemplate.postForEntity(GEMINI_API_URL, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }
            return "AI 답변 추출 실패";

        } catch (Exception e) {
            // 상세한 에러 로그 확인을 위해 전체 메시지 반환
            return "분석 중 에러: " + e.getMessage();
        }
    }
}