package com.evho.usonly.domain.chat.service;

import com.evho.usonly.domain.chat.entity.Chat;
import com.evho.usonly.domain.chat.repository.ChatRepository;
import com.evho.usonly.domain.couple.entity.Couple;
import com.evho.usonly.domain.couple.repository.CoupleRepository;
import com.evho.usonly.global.gemini.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyEmbeddingService {

    private final ChatRepository chatRepository;
    private final CoupleRepository coupleRepository;
    private final GeminiClient geminiClient;
    private final PineconeService pineconeService;

    // 매일 새벽 5시: 커플별로 어제 하루치 대화를 요약+임베딩+upsert
    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
    public void scheduledDailyEmbed() {
        if (!pineconeService.isEnabled()) return;

        String yesterday = LocalDate.now().minusDays(1).toString();
        log.info("일별 임베딩 스케줄 실행: {}", yesterday);
        for (Long coupleId : coupleRepository.findAll().stream().map(Couple::getId).toList()) {
            try {
                embedDay(coupleId, yesterday);
            } catch (Exception e) {
                log.warn("일별 임베딩 실패 (coupleId={}, {}): {}", coupleId, yesterday, e.getMessage());
            }
        }
    }

    // 매일 새벽 5시 30분: 커플별로 최근 30일 중 Pinecone에 없는 날짜 보완
    @Scheduled(cron = "0 30 5 * * *", zone = "Asia/Seoul")
    public void backfillMissingEmbeddings() {
        if (!pineconeService.isEnabled()) return;

        String from = LocalDate.now().minusDays(30).toString();
        for (Long coupleId : coupleRepository.findAll().stream().map(Couple::getId).toList()) {
            List<String> datesWithChat = chatRepository.findDistinctDatesSinceByCoupleId(coupleId, from);
            List<String> missing = datesWithChat.stream()
                    .filter(date -> !pineconeService.existsDay(coupleId, date))
                    .toList();

            if (missing.isEmpty()) continue;

            log.info("백필 대상 (coupleId={}) 날짜 {}건: {}", coupleId, missing.size(), missing);
            for (String date : missing) {
                try {
                    embedDay(coupleId, date);
                    log.info("백필 완료 (coupleId={}): {}", coupleId, date);
                } catch (Exception e) {
                    log.warn("백필 실패 (coupleId={}, {}): {}", coupleId, date, e.getMessage());
                }
            }
        }
    }

    private void embedDay(Long coupleId, String date) {
        if (!pineconeService.isEnabled()) return;

        List<Chat> chats = chatRepository.findByCoupleIdAndDate(coupleId, date);
        String messages = formatMessages(chats);
        if (messages.isBlank()) return;

        // 1. Gemini로 하루 대화 요약 (3~5줄)
        String summary = geminiClient.summarize(messages);

        // 2. [요약] + [원본] 합쳐서 임베딩 텍스트 구성
        String embeddingText = "[요약]\n" + summary + "\n\n[원본 대화]\n" + messages;

        // 3. Gemini Embedding API로 벡터 변환
        List<Float> vector = geminiClient.embed(embeddingText);

        // 4. Pinecone upsert - 커플별 날짜로 저장, 메시지는 MySQL이 원본
        pineconeService.upsertDay(coupleId, date, vector);

        log.info("일별 임베딩 완료 (coupleId={}): {}", coupleId, date);
    }

    private String formatMessages(List<Chat> chats) {
        return chats.stream()
                .filter(c -> c.getMessage() != null && !c.getMessage().startsWith("IMAGE:"))
                .map(c -> String.format("[%s] %s",
                        c.getSendTime() != null ? c.getSendTime() : c.getCreatedAt().toLocalTime().toString(),
                        c.getMessage()))
                .collect(Collectors.joining("\n"));
    }
}
