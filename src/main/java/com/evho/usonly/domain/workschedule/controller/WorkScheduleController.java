package com.evho.usonly.domain.workschedule.controller;

import com.evho.usonly.domain.workschedule.dto.WorkScheduleAnalyzeResponse;
import com.evho.usonly.domain.workschedule.service.WorkScheduleAnalyzeService;
import com.evho.usonly.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/work-schedule")
@RequiredArgsConstructor
public class WorkScheduleController {

    private final WorkScheduleAnalyzeService analyzeService;

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<WorkScheduleAnalyzeResponse>> analyze(@RequestParam("file") MultipartFile file,
                                                                            @RequestParam("name") String name) {
        WorkScheduleAnalyzeResponse result = analyzeService.analyze(file, name);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
