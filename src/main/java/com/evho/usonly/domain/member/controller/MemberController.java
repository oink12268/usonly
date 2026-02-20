package com.evho.usonly.domain.member.controller;

import com.evho.usonly.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/fcm-token")
    public ResponseEntity<String> updateFcmToken(@RequestParam Long userId,
                                                  @RequestParam String token) {
        memberService.updateFcmToken(userId, token);
        return ResponseEntity.ok("토큰 등록 완료");
    }

    @GetMapping("/nickname")
    public ResponseEntity<String> getNickname(@RequestParam String providerId) {
        String nickname = memberService.getNicknameByProviderId(providerId);
        return ResponseEntity.ok(nickname);
    }
}
