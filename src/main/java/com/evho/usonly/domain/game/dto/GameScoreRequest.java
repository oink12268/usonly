package com.evho.usonly.domain.game.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GameScoreRequest {
    private String gameType;
    private int score;
}
