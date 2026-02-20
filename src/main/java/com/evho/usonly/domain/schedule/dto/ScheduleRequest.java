package com.evho.usonly.domain.schedule.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ScheduleRequest {
    private String title;
    private String memo;
    private String date; // "yyyy-MM-dd"
}
