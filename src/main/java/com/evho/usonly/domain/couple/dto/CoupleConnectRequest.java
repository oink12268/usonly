package com.evho.usonly.domain.couple.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CoupleConnectRequest {
    private String myUid;       // 연결을 시도하는 나
    private String partnerCode; // 내가 입력한 상대방 코드
}