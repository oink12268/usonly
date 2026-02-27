package com.evho.usonly.domain.member.controller;

import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.domain.member.service.MemberService;
import com.evho.usonly.global.annotation.CurrentMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/fcm-token")
    public ResponseEntity<String> updateFcmToken(@RequestParam String token,
                                                  @CurrentMember Member me) {
        memberService.updateFcmToken(me.getId(), token);
        return ResponseEntity.ok("토큰 등록 완료");
    }

    @GetMapping("/nickname")
    public ResponseEntity<String> getNickname(@RequestParam String providerId) {
        return ResponseEntity.ok(memberService.getNicknameByProviderId(providerId));
    }
}
