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
                boolean success = false;
                int retries = 0;
                while (!success && retries < 3) {
                    try {
                        dailyEmbeddingService.embedDay(date); // 동기 호출 (진행률 추적)
                        success = true;
                        progress++;
                        log.info("마이그레이션 진행: {}/{} ({})", progress, total, date);
                        Thread.sleep(6000); // 날짜당 Gemini 2회 호출, 분당 10회 제한 대응
                    } catch (org.springframework.web.client.HttpClientErrorException e) {
                        if (e.getStatusCode().value() == 429) {
                            log.warn("{} 임베딩 실패 (일일 할당량 초과) - 마이그레이션 중단. 내일 다시 실행하세요.", date);
                            return; // 일일 한도 초과: 더 진행해도 모두 실패하므로 중단
                        }
                        log.warn("{} 임베딩 실패: {}", date, e.getMessage());
                        break;
                    } catch (org.springframework.web.client.HttpServerErrorException e) {
                        retries++;
                        if (retries < 3) {
                            log.warn("{} 임베딩 실패 (503), {}초 후 재시도 ({}/3)...", date, retries * 30, retries);
                            Thread.sleep(retries * 30000L); // 30s, 60s 대기 후 재시도
                        } else {
                            log.warn("{} 임베딩 실패 (재시도 소진): {}", date, e.getMessage());
                        }
                    } catch (Exception e) {
                        log.warn("{} 임베딩 실패: {}", date, e.getMessage());
                        break;
                    }
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
