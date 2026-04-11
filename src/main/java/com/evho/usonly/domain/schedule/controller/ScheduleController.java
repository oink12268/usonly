package com.evho.usonly.domain.schedule.controller;

import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.domain.schedule.dto.ScheduleRequest;
import com.evho.usonly.domain.schedule.dto.ScheduleResponse;
import com.evho.usonly.domain.schedule.service.ScheduleService;
import com.evho.usonly.global.annotation.CurrentMember;
import com.evho.usonly.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(@CurrentMember Member me,
                                                    @RequestBody ScheduleRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleService.create(me.getId(), body)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getByMonth(@CurrentMember Member me,
                                                                          @RequestParam int year,
                                                                          @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleService.getByMonth(me.getId(), year, month)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable Long id,
                                                    @RequestBody ScheduleRequest request,
                                                    @CurrentMember Member me) {
        scheduleService.update(id, request, me.getId());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @CurrentMember Member me) {
        scheduleService.delete(id, me.getId());
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
