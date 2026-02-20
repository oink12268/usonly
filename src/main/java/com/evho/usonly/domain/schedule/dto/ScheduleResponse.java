package com.evho.usonly.domain.schedule.dto;

import com.evho.usonly.domain.schedule.model.Schedule;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScheduleResponse {
    private Long id;
    private String title;
    private String memo;
    private String date;
    private String writerNickname;

    public static ScheduleResponse of(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getMemo(),
                schedule.getDate().toString(),
                schedule.getWriter() != null ? schedule.getWriter().getNickname() : ""
        );
    }
}
