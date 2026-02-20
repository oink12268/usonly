package com.evho.usonly.domain.member.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Getter, Setter, toString 자동 생성
@NoArgsConstructor // JSON 파싱을 위해 빈 생성자 필요
public class MemberJoinRequest {

    // 화면에서 보내줄 데이터들
    private String uid;
    private String nickname;
    private String email;
}