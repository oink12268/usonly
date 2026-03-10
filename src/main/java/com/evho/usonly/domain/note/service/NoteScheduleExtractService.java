package com.evho.usonly.domain.note.service;

import com.evho.usonly.global.gemini.GeminiClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteScheduleExtractService {

    private final GeminiClient geminiClient;

    private static final String PROMPT_TEMPLATE = """
            아래 메모를 보고, 구글 캘린더에 추가할 수 있게 JSON으로 만들어줘.
            메모에 날짜가 있으면 반드시 찾아서 yyyy-MM-dd 형식으로 date에 넣어줘. 오늘 연도는 %s년이야.
            날짜가 정말 없을 때만 date를 null로 해줘.

            중요: 메모에 시간 정보가 있으면 아래 규칙에 따라 시간 블록 하나당 일정 하나만 만들어.
            시간 앞뒤에 이모지나 특수문자가 있어도 무시하고 시간 패턴만 찾아.
            - "09:00 ~ 11:30" 처럼 시간 범위가 있으면 그대로 startTime, endTime에 넣어.
            - "11:00 대관람차" 처럼 시작 시간만 있으면 endTime은 startTime + 30분으로 계산해서 넣어.
            title은 반드시 "HH:mm ~ HH:mm 장소1, 장소2, 장소3" 형식으로 만들어. (예: "09:00 ~ 11:30 스타페리, 홍콩 대관람차, 케네디타운")
            시작/종료 시간은 해당 블록의 시간을 HH:mm 형식으로 startTime, endTime에 넣어줘.
            description에는 각 장소의 설명을 간략히 넣어줘.
            location에는 해당 블록의 대표 장소명(첫 번째 장소)을 넣어줘. 없으면 null로 해줘.
            다른 설명 없이 JSON 배열만 반환해:
            [{"title":"...","date":"yyyy-MM-dd 또는 null","startTime":"HH:mm 또는 null","endTime":"HH:mm 또는 null","description":"...","location":"...또는 null"}]

            메모:
            %s
            """;

    public List<Map<String, String>> extractSchedules(String noteContent) {
        String year = String.valueOf(LocalDate.now().getYear());
        String prompt = String.format(PROMPT_TEMPLATE, year, noteContent);

        try {
            String json = geminiClient.generateJson(prompt).trim();
            return parseJson(json);
        } catch (Exception e) {
            log.error("Gemini 일정 추출 실패: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> parseJson(String json) {
        try {
            json = json.replaceAll("(?s)```json\\s*", "").replaceAll("```", "").trim();

            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> raw = mapper.readValue(json, new TypeReference<>() {});

            return raw.stream()
                    .filter(m -> m.get("title") != null && !m.get("title").toString().isBlank())
                    .map(m -> {
                        String date = m.get("date") != null ? m.get("date").toString() : null;
                        if (date != null && !date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                            date = null;
                        }
                        return Map.of(
                                "title", m.get("title").toString(),
                                "date", date != null ? date : "",
                                "startTime", m.get("startTime") != null ? m.get("startTime").toString() : "",
                                "endTime", m.get("endTime") != null ? m.get("endTime").toString() : "",
                                "description", m.get("description") != null ? m.get("description").toString() : "",
                                "location", m.get("location") != null ? m.get("location").toString() : ""
                        );
                    })
                    .toList();
        } catch (Exception e) {
            log.error("JSON 파싱 실패: {}, raw: {}", e.getMessage(), json);
            return List.of();
        }
    }
}
