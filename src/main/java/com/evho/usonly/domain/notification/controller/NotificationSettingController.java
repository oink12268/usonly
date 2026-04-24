package com.evho.usonly.domain.notification.controller;

import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.domain.notification.dto.NotificationSettingRequest;
import com.evho.usonly.domain.notification.dto.NotificationSettingResponse;
import com.evho.usonly.domain.notification.service.NotificationSettingService;
import com.evho.usonly.global.annotation.CurrentMember;
import com.evho.usonly.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification-settings")
@RequiredArgsConstructor
public class NotificationSettingController {

    private final NotificationSettingService service;

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> get(@CurrentMember Member member) {
        NotificationSettingResponse resp = NotificationSettingResponse.of(service.getOrCreate(member));
        return ResponseEntity.ok(ApiResponse.ok(resp));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> update(
            @CurrentMember Member member,
            @RequestBody NotificationSettingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(member, request)));
    }
}
