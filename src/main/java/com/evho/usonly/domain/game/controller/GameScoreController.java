package com.evho.usonly.domain.game.controller;

import com.evho.usonly.domain.game.dto.GameScoreEntryResponse;
import com.evho.usonly.domain.game.dto.GameScoreRequest;
import com.evho.usonly.domain.game.dto.GameScoreSummaryResponse;
import com.evho.usonly.domain.game.service.GameScoreService;
import com.evho.usonly.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/game/scores")
public class GameScoreController {

    private final GameScoreService gameScoreService;

    @PostMapping
    public ApiResponse<GameScoreSummaryResponse> submitScore(
            @RequestAttribute("firebaseUid") String firebaseUid,
            @RequestBody GameScoreRequest req) {
        return ApiResponse.created(gameScoreService.submitScore(firebaseUid, req));
    }

    @GetMapping("/top10")
    public ApiResponse<List<GameScoreEntryResponse>> getTop10(
            @RequestAttribute("firebaseUid") String firebaseUid,
            @RequestParam(defaultValue = "flappy") String gameType) {
        return ApiResponse.ok(gameScoreService.getTop10(firebaseUid, gameType));
    }
}
