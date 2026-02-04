package com.evho.usonly.domain.couple.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Couple {
    private String coupleId;     // 커플방 ID (자동생성)
    private List<String> members; // [남편UID, 아내UID]
    private String startDate;    // 사귄 날짜 (나중에 D-Day 계산용, 일단 String으로 저장)
    private String createdAt;    // 방 생성일
}