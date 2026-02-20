package com.evho.usonly.domain.couple.controller;

import com.evho.usonly.domain.couple.service.CoupleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

//
@RestController
@RequestMapping("/api/couples")
@RequiredArgsConstructor
public class CoupleController {

    private final CoupleService coupleService;

    // 커플 연결 요청 API
    @PostMapping("/connect")
    public ResponseEntity<?> connect(@RequestBody Map<String, String> body) {
        String myEmail = body.get("email");       // 내 이메일 (누가 요청했는지 알아야 하니까)
        String partnerCode = body.get("code");    // 입력한 상대방 코드

        try {
            Long test = 1l;
            Long coupleId = coupleService.connectCouple(myEmail, partnerCode);
            return ResponseEntity.ok(Map.of("message", "매칭 성공!", "coupleId", coupleId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}