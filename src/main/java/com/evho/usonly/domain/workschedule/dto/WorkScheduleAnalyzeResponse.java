package com.evho.usonly.domain.workschedule.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class WorkScheduleAnalyzeResponse {
    private String name;
    private List<DaySchedule> schedules;
    private String ocrText; // 디버깅용

    @Getter
    @AllArgsConstructor
    public static class DaySchedule {
        private String date;
        private String dayOfWeek;
        private String shift;
    }
}
