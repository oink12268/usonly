package com.evho.usonly.domain.member.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder // 1. 빌더 패턴 활성화
@NoArgsConstructor // 2. Firestore용 빈 생성자 (필수!)
@AllArgsConstructor // 3. 빌더가 내부적으로 쓰는 전체 생성자 (필수!)
public class User {

    // 1. 기본 정보
    private String uid;          // 로그인한 유저의 고유 ID (카카오/구글 등에서 줌)
    private String nickname;     // 화면에 보여질 이름
    private String email;        // 유저의 이메일

    // 2. 매칭용 핵심 정보
    private String myCode;       // 내 초대 코드 (예: A1B2C3)
    private String status;       // 현재 상태 (SOLO, COUPLE)

    // 3. 연결된 정보 (매칭 전엔 null)
    private String partnerUid;   // 아내의 UID
    private String coupleId;     // 우리 둘만의 방 ID
}