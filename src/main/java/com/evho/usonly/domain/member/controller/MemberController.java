package com.evho.usonly.domain.member.controller;

import com.evho.usonly.domain.member.dto.MemberJoinRequest; // DTO import
import com.evho.usonly.domain.member.model.User;
import com.evho.usonly.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@CrossOrigin("*")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/join")
    public User join(@RequestBody MemberJoinRequest request) {
        try {
            // 낱개로 꺼내지 않고, 받은 DTO 그대로 서비스에 던집니다.
            return memberService.join(request);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @GetMapping("/{uid}")
    public User getMember(@PathVariable String uid) {
        try {
            return memberService.getMember(uid);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}