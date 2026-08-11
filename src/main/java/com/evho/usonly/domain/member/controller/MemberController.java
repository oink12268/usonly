package com.evho.usonly.domain.member.controller;

import com.evho.usonly.domain.member.dto.MemberInfoResponse;
import com.evho.usonly.domain.member.dto.MemberPublicResponse;
import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.domain.member.service.MemberDeletionService;
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
    private final MemberDeletionService memberDeletionService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberInfoResponse>> getMyInfo(@CurrentMember Member me) {
        return ResponseEntity.ok(ApiResponse.ok(memberService.getMyInfo(me.getId())));
    }

    // 회원탈퇴: 커플 공유 데이터(채팅/앨범/메모/일정/기념일/쿠폰)까지 전부 삭제되고 되돌릴 수 없음.
    // 파트너 계정 자체는 유지되고 커플 연결만 해제됨.
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@CurrentMember Member me) {
        memberDeletionService.deleteAccount(me);
        return ResponseEntity.ok(ApiResponse.ok());
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
    public ResponseEntity<ApiResponse<MemberPublicResponse>> getMemberInfo(@RequestParam String providerId) {
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
