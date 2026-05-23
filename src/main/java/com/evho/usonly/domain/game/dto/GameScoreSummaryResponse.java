package com.evho.usonly.domain.game.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GameScoreSummaryResponse {
    private int myBest;
    private Integer partnerBest;
    private String partnerNickname;
    private String partnerProfileImageUrl;
}
