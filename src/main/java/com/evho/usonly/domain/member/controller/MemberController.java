package com.evho.usonly.domain.member.controller;

import com.evho.usonly.domain.member.dto.MemberInfoResponse;
import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.domain.member.service.MemberService;
import com.evho.usonly.global.annotation.CurrentMember;
import com.evho.usonly.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberInfoResponse>> getMyInfo(@CurrentMember Member me) {
        return ResponseEntity.ok(ApiResponse.ok(memberService.getMyInfo(me.getId())));
    }

    @PutMapping("/nickname")
    public ResponseEntity<ApiResponse<Void>> updateNickname(@RequestParam String nickname,
                                                            @CurrentMember Member me) {
        memberService.updateNickname(me.getId(), nickname);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/profile-image")
    public ResponseEntity<ApiResponse<String>> updateProfileImage(@RequestParam("file") MultipartFile file,
                                                                  @CurrentMember Member me) {
        String imageUrl = memberService.updateProfileImage(me.getId(), file);
        return ResponseEntity.ok(ApiResponse.ok(imageUrl));
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<MemberInfoResponse>> getMemberInfo(@RequestParam String providerId) {
        return ResponseEntity.ok(ApiResponse.ok(memberService.getMemberInfoByProviderId(providerId)));
    }

    @PostMapping("/fcm-token")
    public ResponseEntity<ApiResponse<Void>> updateFcmToken(@RequestParam String token,
                                                            @CurrentMember Member me) {
        memberService.updateFcmToken(me.getId(), token);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @GetMapping("/nickname")
    public ResponseEntity<ApiResponse<String>> getNickname(@RequestParam String providerId) {
        return ResponseEntity.ok(ApiResponse.ok(memberService.getNicknameByProviderId(providerId)));
    }
}
