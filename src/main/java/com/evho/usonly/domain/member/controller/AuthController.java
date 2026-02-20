package com.evho.usonly.domain.member.controller;

import com.evho.usonly.domain.member.dto.LoginDto;
import com.evho.usonly.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;

    @PostMapping("/login")
    public ResponseEntity<LoginDto.LoginResponse> login(@RequestBody LoginDto.LoginRequest request) {
        LoginDto.LoginResponse response = memberService.loginOrSignup(request);
        return ResponseEntity.ok(response);
    }
}
