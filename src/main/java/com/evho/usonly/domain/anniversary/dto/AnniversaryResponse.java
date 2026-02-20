package com.evho.usonly.domain.anniversary.dto;

import com.evho.usonly.domain.anniversary.model.Anniversary;
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

    public static AnniversaryResponse of(Anniversary anniversary) {
        LocalDate today = LocalDate.now();
        LocalDate targetDate = anniversary.getDate();

        // 반복 기념일이면 올해(또는 내년) 날짜 기준으로 계산
        if (anniversary.isRecurring()) {
            targetDate = targetDate.withYear(today.getYear());
            if (targetDate.isBefore(today)) {
                targetDate = targetDate.plusYears(1);
            }
        }

        long dday = ChronoUnit.DAYS.between(today, targetDate);

        return new AnniversaryResponse(
                anniversary.getId(),
                anniversary.getTitle(),
                anniversary.getDate().toString(),
                anniversary.isRecurring(),
                dday
        );
    }
}
