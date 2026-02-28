package com.evho.usonly.domain.member.dto;

import com.evho.usonly.domain.member.entity.Member;
import lombok.Getter;

@Getter
public class MemberInfoResponse {

    private final Long id;
    private final String nickname;
    private final String profileImageUrl;
    private final String email;

    private MemberInfoResponse(Member member) {
        this.id = member.getId();
        this.nickname = member.getNickname();
        this.profileImageUrl = member.getProfileImageUrl();
        this.email = member.getEmail();
    }

    public static MemberInfoResponse from(Member member) {
        return new MemberInfoResponse(member);
    }
}
