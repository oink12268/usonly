package com.evho.usonly.domain.anniversary.controller;

import com.evho.usonly.domain.anniversary.dto.AnniversaryRequest;
import com.evho.usonly.domain.anniversary.dto.AnniversaryResponse;
import com.evho.usonly.domain.anniversary.service.AnniversaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/anniversaries")
@RequiredArgsConstructor
public class AnniversaryController {

    private final AnniversaryService anniversaryService;

    @PostMapping
    public ResponseEntity<Long> create(@RequestParam Long userId,
                                       @RequestBody AnniversaryRequest request) {
        Long id = anniversaryService.create(userId, request);
        return ResponseEntity.ok(id);
    }

    @GetMapping
    public ResponseEntity<List<AnniversaryResponse>> getAll(@RequestParam Long userId) {
        return ResponseEntity.ok(anniversaryService.getAll(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(@PathVariable Long id,
                                         @RequestBody AnniversaryRequest request) {
        anniversaryService.update(id, request);
        return ResponseEntity.ok("수정 완료");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        anniversaryService.delete(id);
        return ResponseEntity.ok("삭제 완료");
    }
}
