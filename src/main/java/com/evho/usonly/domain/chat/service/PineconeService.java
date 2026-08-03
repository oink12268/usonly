package com.evho.usonly.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PineconeService {

    private final RestTemplate restTemplate;

    @Value("${pinecone.api-key:}")
    private String apiKey;

    @Value("${pinecone.index-host:}")
    private String indexHost;

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank()
                && indexHost != null && !indexHost.isBlank();
    }

    // 일별 청킹: ID = "day-{coupleId}-2024-01-15", 커플별로 분리 저장 (메시지는 MySQL이 원본)
    public void upsertDay(Long coupleId, String date, List<Float> vector) {
        if (!isEnabled()) return;

        HttpHeaders headers = headers();
        Map<String, Object> point = Map.of(
                "id", vectorId(coupleId, date),
                "values", vector,
                "metadata", Map.of("date", date, "coupleId", coupleId)
        );

        restTemplate.postForEntity(
                indexHost + "/vectors/upsert",
                new HttpEntity<>(Map.of("vectors", List.of(point)), headers),
                Map.class
        );
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> query(Long coupleId, List<Float> queryVector, int topK) {
        if (!isEnabled()) return List.of();

        HttpHeaders headers = headers();
        Map<String, Object> body = Map.of(
                "vector", queryVector,
                "topK", topK,
                "includeMetadata", true,
                "filter", Map.of("coupleId", Map.of("$eq", coupleId))
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                indexHost + "/query",
                new HttpEntity<>(body, headers),
                Map.class
        );

        if (response.getBody() == null) return List.of();
        List<Map<String, Object>> matches = (List<Map<String, Object>>) response.getBody().get("matches");
        return matches != null ? matches : List.of();
    }

    // 날짜가 이미 임베딩됐는지 확인
    public boolean existsDay(Long coupleId, String date) {
        if (!isEnabled()) return false;
        try {
            String id = vectorId(coupleId, date);
            ResponseEntity<Map> response = restTemplate.exchange(
                    indexHost + "/vectors/fetch?ids=" + id,
                    HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    Map.class
            );
            if (response.getBody() == null) return false;
            Map<?, ?> vectors = (Map<?, ?>) response.getBody().get("vectors");
            return vectors != null && vectors.containsKey(id);
        } catch (Exception e) {
            log.warn("Pinecone fetch 실패 (coupleId={}, {}): {}", coupleId, date, e.getMessage());
            return false;
        }
    }

    private String vectorId(Long coupleId, String date) {
        return "day-" + coupleId + "-" + date;
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Api-Key", apiKey);
        return headers;
    }
}
