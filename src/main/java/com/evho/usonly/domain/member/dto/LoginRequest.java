package com.evho.usonly.domain.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    private String email;
    private String nickname;
    private String provider;   // "KAKAO"
    private String providerId; // 카카오 고유 ID
    private String profileImageUrl;
}
