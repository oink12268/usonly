package com.evho.usonly.domain.chat.service;

import com.evho.usonly.domain.chat.entity.Chat;
import com.evho.usonly.domain.chat.repository.ChatRepository;
import com.evho.usonly.global.gemini.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatSearchService {

    private final ChatRepository chatRepository;
    private final GeminiClient geminiClient;
    private final PineconeService pineconeService;

    @Cacheable(value = "aiSearch", key = "#coupleId + '_' + #query + '_' + T(java.time.LocalDate).now()", unless = "#result.startsWith('분석 중 에러')")
    public String search(Long coupleId, String query) {
        String chatHistory = pineconeService.isEnabled()
                ? searchWithRag(coupleId, query)
                : searchWithFullHistory(coupleId);

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
        log.info("프롬프트 완성::prompt: {}", prompt);

        try {
            return geminiClient.generate(prompt);
        } catch (Exception e) {
            return "분석 중 에러: " + e.getMessage();
        }
    }

    // RAG: Pinecone에서 coupleId로 필터링해 유사한 날짜 Top-5 검색 → MySQL에서 실제 메시지 조회
    @SuppressWarnings("unchecked")
    private String searchWithRag(Long coupleId, String query) {
        try {
            List<Float> queryVector = geminiClient.embed(query);
            List<Map<String, Object>> matches = pineconeService.query(coupleId, queryVector, 5);

            return matches.stream()
                    .map(m -> {
                        String date = (String) ((Map<String, Object>) m.get("metadata")).get("date");
                        String messages = chatRepository.findByCoupleIdAndDate(coupleId, date).stream()
                                .filter(c -> c.getMessage() != null && !c.getMessage().startsWith("IMAGE:"))
                                .map(c -> "[" + c.getSendTime() + "] " + c.getMessage())
                                .collect(Collectors.joining("\n"));
                        return "=== " + date + " ===\n" + messages;
                    })
                    .collect(Collectors.joining("\n\n"));
        } catch (Exception e) {
            log.warn("RAG 검색 실패, 전체 히스토리로 폴백: {}", e.getMessage());
            return searchWithFullHistory(coupleId);
        }
    }

    // 폴백: Pinecone 미설정 시 기존 방식 (해당 커플 전체 로드)
    private String searchWithFullHistory(Long coupleId) {
        List<Chat> chats = chatRepository.findByCoupleIdOrderByCreatedAtAsc(coupleId);
        return chats.stream()
                .filter(c -> c.getMessage() != null && !c.getMessage().startsWith("IMAGE:"))
                .map(c -> String.format("[%s] %s: %s",
                        c.getCreatedAt(),
                        c.getWriterUid().substring(0, Math.min(6, c.getWriterUid().length())),
                        c.getMessage()))
                .collect(Collectors.joining("\n"));
    }
}
