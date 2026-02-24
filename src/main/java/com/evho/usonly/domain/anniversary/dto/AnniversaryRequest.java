package com.evho.usonly.domain.anniversary.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AnniversaryRequest {
    private String title;
    private String date; // "yyyy-MM-dd" (음력이면 null 가능)
    private boolean recurring;
    private boolean lunar;
    private Integer lunarMonth;
    private Integer lunarDay;
}
