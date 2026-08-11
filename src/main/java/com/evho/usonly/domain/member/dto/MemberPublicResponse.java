package com.evho.usonly.domain.member.dto;

import com.evho.usonly.domain.member.entity.Member;
import lombok.Getter;

// providerId로 아무 멤버나 조회하는 공개 엔드포인트용. 표시에 필요한 최소 정보만 노출하고
// 이메일 등 PII는 제외한다. (본인 정보 조회는 MemberInfoResponse 사용)
@Getter
public class MemberPublicResponse {

    private final Long id;
    private final String nickname;
    private final String profileImageUrl;

    private MemberPublicResponse(Member member) {
        this.id = member.getId();
        this.nickname = member.getNickname();
        this.profileImageUrl = member.getProfileImageUrl();
    }

    public static MemberPublicResponse from(Member member) {
        return new MemberPublicResponse(member);
    }
}
