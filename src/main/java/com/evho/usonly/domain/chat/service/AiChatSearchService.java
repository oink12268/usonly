package com.evho.usonly.domain.chat.service;

import com.evho.usonly.domain.chat.entity.Chat;
import com.evho.usonly.domain.chat.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatSearchService {

    private final ChatRepository chatRepository;
    private final GeminiEmbeddingService embeddingService;
    private final PineconeService pineconeService;

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

    @SuppressWarnings("unchecked")
    @Cacheable(value = "aiSearch", key = "#query", unless = "#result.startsWith('분석 중 에러')")
    public String search(String query) {
        String chatHistory = pineconeService.isEnabled()
                ? searchWithRag(query)
                : searchWithFullHistory();

        if (chatHistory == null || chatHistory.isBlank()) {
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

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", geminiApiKey);

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    )
            );

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    GEMINI_API_URL, new HttpEntity<>(body, headers), Map.class
            );

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
            return "분석 중 에러: " + e.getMessage();
        }
    }

    // RAG: Pinecone에서 유사한 날짜 Top-5 검색 → MySQL에서 실제 메시지 조회
    @SuppressWarnings("unchecked")
    private String searchWithRag(String query) {
        try {
            List<Float> queryVector = embeddingService.embed(query);
            List<Map<String, Object>> matches = pineconeService.query(queryVector, 5);

            return matches.stream()
                    .map(m -> {
                        String date = (String) ((Map<String, Object>) m.get("metadata")).get("date");
                        String messages = chatRepository.findByDate(date).stream()
                                .filter(c -> c.getMessage() != null && !c.getMessage().startsWith("IMAGE:"))
                                .map(c -> "[" + c.getSendTime() + "] " + c.getMessage())
                                .collect(Collectors.joining("\n"));
                        return "=== " + date + " ===\n" + messages;
                    })
                    .collect(Collectors.joining("\n\n"));
        } catch (Exception e) {
            log.warn("RAG 검색 실패, 전체 히스토리로 폴백: {}", e.getMessage());
            return searchWithFullHistory();
        }
    }

    // 폴백: Pinecone 미설정 시 기존 방식 (전체 로드)
    private String searchWithFullHistory() {
        List<Chat> chats = chatRepository.findAllByOrderByCreatedAtAsc();
        return chats.stream()
                .filter(c -> c.getMessage() != null && !c.getMessage().startsWith("IMAGE:"))
                .map(c -> String.format("[%s] %s: %s",
                        c.getCreatedAt(),
                        c.getWriterUid().substring(0, Math.min(6, c.getWriterUid().length())),
                        c.getMessage()))
                .collect(Collectors.joining("\n"));
    }
}
