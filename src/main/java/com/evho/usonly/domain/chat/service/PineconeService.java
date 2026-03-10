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

    // 일별 청킹: ID = "day-2024-01-15", 날짜만 저장 (메시지는 MySQL이 원본)
    public void upsertDay(String date, List<Float> vector) {
        if (!isEnabled()) return;

        HttpHeaders headers = headers();
        Map<String, Object> point = Map.of(
                "id", "day-" + date,
                "values", vector,
                "metadata", Map.of("date", date)
        );

        restTemplate.postForEntity(
                indexHost + "/vectors/upsert",
                new HttpEntity<>(Map.of("vectors", List.of(point)), headers),
                Map.class
        );
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> query(List<Float> queryVector, int topK) {
        if (!isEnabled()) return List.of();

        HttpHeaders headers = headers();
        Map<String, Object> body = Map.of(
                "vector", queryVector,
                "topK", topK,
                "includeMetadata", true
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

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Api-Key", apiKey);
        return headers;
    }
}
