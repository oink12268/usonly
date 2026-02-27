package com.evho.usonly.domain.couple.controller;

import com.evho.usonly.domain.couple.service.CoupleService;
import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.global.annotation.CurrentMember;
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
    public ResponseEntity<?> connect(@CurrentMember Member me,
                                     @RequestBody Map<String, String> body) {
        String partnerCode = body.get("code");
        try {
            Long coupleId = coupleService.connectCouple(me.getId(), partnerCode);
            return ResponseEntity.ok(Map.of(
                    "message", "매칭 성공!",
                    "coupleId", coupleId,
                    "id", me.getId()
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
