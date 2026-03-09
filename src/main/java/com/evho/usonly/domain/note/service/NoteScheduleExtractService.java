package com.evho.usonly.domain.note.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NoteScheduleExtractService {

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    private static final String PROMPT_TEMPLATE = """
            아래 메모를 보고, 시간을 제목에 넣어서 구글 캘린더에 추가할 수 있게 JSON으로 만들어줘.
            메모에 날짜가 있으면 반드시 찾아서 yyyy-MM-dd 형식으로 date에 넣어줘. 오늘 연도는 %s년이야.
            날짜가 정말 없을 때만 date를 null로 해줘.
            일정이 여러 개면 모두 포함시켜.
            다른 설명 없이 JSON 배열만 반환해:
            [{"title":"...","date":"yyyy-MM-dd 또는 null","description":"..."}]

            메모:
            %s
            """;

    /**
     * 메모 내용에서 일정 목록을 추출합니다.
     * @return 추출된 이벤트 목록. 각 항목은 title, date(nullable), description 포함
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> extractSchedules(String noteContent) {
        String year = String.valueOf(LocalDate.now().getYear());
        String prompt = String.format(PROMPT_TEMPLATE, year, noteContent);

        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", geminiApiKey);

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0,
                            "responseMimeType", "application/json"
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
                        String json = ((String) parts.get(0).get("text")).trim();
                        return parseJson(json);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Gemini 일정 추출 실패: {}", e.getMessage());
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> parseJson(String json) {
        try {
            // 코드블록이 붙어 올 경우 제거
            json = json.replaceAll("(?s)```json\\s*", "").replaceAll("```", "").trim();

            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> raw = mapper.readValue(json, new TypeReference<>() {});

            return raw.stream()
                    .filter(m -> m.get("title") != null && !m.get("title").toString().isBlank())
                    .map(m -> {
                        String date = m.get("date") != null ? m.get("date").toString() : null;
                        // 날짜 형식 검증 - 틀리면 null 처리
                        if (date != null && !date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                            date = null;
                        }
                        String desc = m.get("description") != null ? m.get("description").toString() : "";
                        return Map.of(
                                "title", m.get("title").toString(),
                                "date", date != null ? date : "",
                                "description", desc
                        );
                    })
                    .toList();
        } catch (Exception e) {
            log.error("JSON 파싱 실패: {}, raw: {}", e.getMessage(), json);
            return List.of();
        }
    }
}
