package com.evho.usonly.domain.member.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public class LoginDto {
    @Getter
    @Setter
    public static class LoginRequest {
        private String email;
        private String nickname;
        private String provider;   // "KAKAO"
        private String providerId; // 카카오 고유 ID
        private String profileImageUrl;
    }

    // 응답: 서버가 돌려줄 정보 (초대코드 포함!)
    @Getter @Builder
    public static class LoginResponse {
        private Long memberId;
        private String email;
        private String nickname;
        private String invitationCode; // ★ 핵심: 이걸 프론트에 줘야 함
        private Long coupleId;         // 커플 여부 확인용 (null이면 솔로)
        private String token;          // JWT (나중에 필요하면 구현, 일단은 패스)
    }
}
