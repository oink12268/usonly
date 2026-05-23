package com.evho.usonly.domain.game.dto;

import com.evho.usonly.domain.game.entity.GameScore;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class GameScoreEntryResponse {
    private int rank;
    private String nickname;
    private String profileImageUrl;
    private int score;
    private LocalDateTime playedAt;
    private boolean isMe;

    public static GameScoreEntryResponse of(int rank, GameScore gs, boolean isMe) {
        return new GameScoreEntryResponse(
                rank,
                gs.getMember().getNickname(),
                gs.getMember().getProfileImageUrl(),
                gs.getScore(),
                gs.getPlayedAt(),
                isMe
        );
    }
}
