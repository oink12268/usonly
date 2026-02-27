package com.evho.usonly.domain.anniversary.dto;

import com.evho.usonly.domain.anniversary.entity.Anniversary;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Getter
@AllArgsConstructor
public class AnniversaryResponse {
    private Long id;
    private String title;
    private String date;
    private boolean recurring;
    private long dday; // 남은 일수 (음수면 지남, 0이면 오늘)
    private boolean lunar;
    private Integer lunarMonth;
    private Integer lunarDay;

    public static AnniversaryResponse of(Anniversary anniversary, LocalDate nextSolarDate) {
        LocalDate today = LocalDate.now();
        long dday = nextSolarDate != null ? ChronoUnit.DAYS.between(today, nextSolarDate) : 0;

        return new AnniversaryResponse(
                anniversary.getId(),
                anniversary.getTitle(),
                anniversary.getDate().toString(),
                anniversary.isRecurring(),
                dday,
                anniversary.isLunar(),
                anniversary.getLunarMonth(),
                anniversary.getLunarDay()
        );
    }
}
