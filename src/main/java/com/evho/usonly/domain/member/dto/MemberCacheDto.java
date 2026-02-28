package com.evho.usonly.domain.member.dto;

import com.evho.usonly.domain.member.entity.Member;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberCacheDto {
    private Long id;
    private String nickname;
    private String fcmToken;
    private Long coupleId;

    public static MemberCacheDto from(Member member) {
        return MemberCacheDto.builder()
                .id(member.getId())
                .nickname(member.getNickname())
                .fcmToken(member.getFcmToken())
                .coupleId(member.getCouple() != null ? member.getCouple().getId() : null)
                .build();
    }
}
