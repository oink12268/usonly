package com.evho.usonly.domain.member.dto;

import com.evho.usonly.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberCacheDto {
    private Long id;
    private String nickname;
    private String profileImageUrl;
    private String fcmToken;
    private Long coupleId;

    public static MemberCacheDto from(Member member) {
        return MemberCacheDto.builder()
                .id(member.getId())
                .nickname(member.getNickname())
                .profileImageUrl(member.getProfileImageUrl())
                .fcmToken(member.getFcmToken())
                .coupleId(member.getCouple() != null ? member.getCouple().getId() : null)
                .build();
    }
}
