package com.evho.usonly.domain.anniversary.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AnniversaryRequest {
    private String title;
    private String date; // "yyyy-MM-dd"
    private boolean recurring;
}
