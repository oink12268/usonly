package com.evho.usonly.domain.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GeminiEmbeddingService {

    @Value("${gemini.api-key}")
    private String apiKey;

    private static final String EMBEDDING_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent";

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GENERATE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

    @SuppressWarnings("unchecked")
    public List<Float> embed(String text) {
        // text-embedding-004 최대 2048 토큰, 한국어 기준 약 5000자 제한
        String truncated = text.length() > 5000 ? text.substring(0, 5000) : text;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        Map<String, Object> body = Map.of(
                "content", Map.of("parts", List.of(Map.of("text", truncated))),
                "output_dimensionality", 768  // 기존 DB 차원과 맞추기 위해 768 명시
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                EMBEDDING_URL, new HttpEntity<>(body, headers), Map.class
        );

        Map<String, Object> embedding = (Map<String, Object>) response.getBody().get("embedding");
        List<Double> values = (List<Double>) embedding.get("values");
        return values.stream().map(Double::floatValue).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public String summarize(String conversationText) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        String prompt = "다음은 커플의 하루 채팅 내역이야. 이 대화의 핵심 내용을 3~5줄로 요약해줘. " +
                "어디 갔는지, 뭘 먹었는지, 무슨 약속을 했는지 등 나중에 검색에 유용한 키워드 위주로 써줘. " +
                "요약만 출력하고 다른 말은 하지 마.\n\n" + conversationText;

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                GENERATE_URL, new HttpEntity<>(body, headers), Map.class
        );

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        return (String) parts.get(0).get("text");
    }
}
