package com.evho.usonly.domain.couple.controller;

import com.evho.usonly.domain.couple.dto.CoupleConnectRequest;
import com.evho.usonly.domain.couple.service.CoupleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/couples")
@RequiredArgsConstructor
public class CoupleController {

    private final CoupleService coupleService;

    @PostMapping("/connect")
    public String connect(@RequestBody CoupleConnectRequest request) {
        try {
            return coupleService.connect(request);
        } catch (Exception e) {
            e.printStackTrace();
            return "매칭 중 오류 발생: " + e.getMessage();
        }
    }
}