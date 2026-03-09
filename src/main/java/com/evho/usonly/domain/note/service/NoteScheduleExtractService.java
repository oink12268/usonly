package com.evho.usonly.domain.note.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class NoteScheduleExtractService {

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    /**
     * 메모 내용에서 일정 정보를 추출합니다.
     * @return Map with keys: title, date (yyyy-MM-dd), description
     *         or null if no schedule found
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> extractSchedule(String noteContent) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String prompt = String.format(
                "다음 메모에서 일정 정보를 추출해줘. 오늘 날짜는 %s야.\n\n" +
                "메모:\n%s\n\n" +
                "응답 형식 (반드시 아래 형식만 사용, 다른 텍스트 없이):\n" +
                "TITLE: [일정 제목]\n" +
                "DATE: [yyyy-MM-dd 형식의 날짜]\n" +
                "DESCRIPTION: [간단한 설명 또는 없으면 비워두기]\n\n" +
                "일정을 찾지 못했으면 첫 줄에 NO_SCHEDULE 이라고만 답해.",
                today, noteContent
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
                        String text = (String) parts.get(0).get("text");
                        return parseResponse(text.trim());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Gemini 일정 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    private Map<String, String> parseResponse(String text) {
        if (text.startsWith("NO_SCHEDULE")) return null;

        String title = extract(text, "TITLE");
        String date = extract(text, "DATE");
        String description = extract(text, "DESCRIPTION");

        if (title == null || date == null) return null;

        // 날짜 형식 검증
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) return null;

        return Map.of(
                "title", title,
                "date", date,
                "description", description != null ? description : ""
        );
    }

    private String extract(String text, String key) {
        Pattern pattern = Pattern.compile("(?m)^" + key + ":\\s*(.*)$");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String value = matcher.group(1).trim();
            return value.isEmpty() ? null : value;
        }
        return null;
    }
}
