package com.evho.usonly.domain.chat.service;

import com.evho.usonly.domain.chat.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMigrationService {

    private final ChatRepository chatRepository;
    private final DailyEmbeddingService dailyEmbeddingService;
    private final PineconeService pineconeService;

    private volatile boolean running = false;
    private volatile int progress = 0;
    private volatile int total = 0;

    @Async
    public void migrate() {
        if (running) {
            log.info("마이그레이션이 이미 실행 중입니다.");
            return;
        }
        running = true;
        progress = 0;

        try {
            // 채팅이 존재하는 날짜 목록 조회
            List<Object[]> rows = chatRepository.findChatCountByDate();
            List<String> dates = rows.stream()
                    .map(r -> r[0].toString())
                    .toList();

            total = dates.size();
            log.info("일별 임베딩 마이그레이션 시작: 총 {}일", total);

            for (String date : dates) {
                try {
                    dailyEmbeddingService.embedDay(date); // 동기 호출 (진행률 추적)
                    progress++;
                    log.info("마이그레이션 진행: {}/{} ({})", progress, total, date);
                    Thread.sleep(1500); // 요약(Gemini) + 임베딩 API 연속 호출 간격
                } catch (Exception e) {
                    log.warn("{} 임베딩 실패: {}", date, e.getMessage());
                }
            }
            log.info("마이그레이션 완료: {}/{}일", progress, total);
        } catch (Exception e) {
            log.error("마이그레이션 중 오류 발생", e);
        } finally {
            running = false;
        }
    }

    public Map<String, Object> getStatus() {
        return Map.of(
                "running", running,
                "progress", progress,
                "total", total,
                "pineconeEnabled", pineconeService.isEnabled()
        );
    }
}
