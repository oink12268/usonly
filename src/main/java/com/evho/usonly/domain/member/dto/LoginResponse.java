package com.evho.usonly.domain.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
    private Long memberId;
    private String email;
    private String nickname;
    private String invitationCode;
    private Long coupleId;
    private String token;
}
