package com.evho.usonly.domain.couple.controller;

import com.evho.usonly.domain.couple.dto.CoupleConnectResponse;
import com.evho.usonly.domain.couple.service.CoupleService;
import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.global.annotation.CurrentMember;
import com.evho.usonly.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/couples")
@RequiredArgsConstructor
public class CoupleController {

    private final CoupleService coupleService;

    @PostMapping("/connect")
    public ResponseEntity<ApiResponse<CoupleConnectResponse>> connect(@CurrentMember Member me,
                                                                      @RequestBody Map<String, String> body) {
        String partnerCode = body.get("code");
        Long coupleId = coupleService.connectCouple(me.getId(), partnerCode);
        return ResponseEntity.ok(ApiResponse.ok(new CoupleConnectResponse(coupleId, me.getId())));
    }
}
