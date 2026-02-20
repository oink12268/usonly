package com.evho.usonly.domain.schedule.controller;

import com.evho.usonly.domain.schedule.dto.ScheduleRequest;
import com.evho.usonly.domain.schedule.dto.ScheduleResponse;
import com.evho.usonly.domain.schedule.service.ScheduleService;
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
    public ResponseEntity<Long> create(@RequestParam Long userId,
                                       @RequestBody ScheduleRequest request) {
        Long id = scheduleService.create(userId, request);
        return ResponseEntity.ok(id);
    }

    @GetMapping
    public ResponseEntity<List<ScheduleResponse>> getByMonth(@RequestParam Long userId,
                                                              @RequestParam int year,
                                                              @RequestParam int month) {
        return ResponseEntity.ok(scheduleService.getByMonth(userId, year, month));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(@PathVariable Long id,
                                         @RequestBody ScheduleRequest request) {
        scheduleService.update(id, request);
        return ResponseEntity.ok("수정 완료");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return ResponseEntity.ok("삭제 완료");
    }
}
