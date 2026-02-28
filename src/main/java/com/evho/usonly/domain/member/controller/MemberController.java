package com.evho.usonly.domain.member.controller;

import com.evho.usonly.domain.member.dto.MemberInfoResponse;
import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.domain.member.service.MemberService;
import com.evho.usonly.global.annotation.CurrentMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 내 프로필 조회
    @GetMapping("/me")
    public ResponseEntity<MemberInfoResponse> getMyInfo(@CurrentMember Member me) {
        return ResponseEntity.ok(memberService.getMyInfo(me.getId()));
    }

    // 닉네임 변경
    @PutMapping("/nickname")
    public ResponseEntity<String> updateNickname(@RequestParam String nickname,
                                                  @CurrentMember Member me) {
        memberService.updateNickname(me.getId(), nickname);
        return ResponseEntity.ok("닉네임 변경 완료");
    }

    // 프로필 이미지 업로드
    @PostMapping("/profile-image")
    public ResponseEntity<String> updateProfileImage(@RequestParam("file") MultipartFile file,
                                                      @CurrentMember Member me) {
        try {
            String imageUrl = memberService.updateProfileImage(me.getId(), file);
            return ResponseEntity.ok(imageUrl);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("실패: " + e.getMessage());
        }
    }

    // 채팅에서 상대방 프로필 조회 (providerId 기준)
    @GetMapping("/info")
    public ResponseEntity<MemberInfoResponse> getMemberInfo(@RequestParam String providerId) {
        return ResponseEntity.ok(memberService.getMemberInfoByProviderId(providerId));
    }

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
