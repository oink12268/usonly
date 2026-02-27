package com.evho.usonly.domain.anniversary.controller;

import com.evho.usonly.domain.anniversary.dto.AnniversaryRequest;
import com.evho.usonly.domain.anniversary.dto.AnniversaryResponse;
import com.evho.usonly.domain.anniversary.service.AnniversaryService;
import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.global.annotation.CurrentMember;
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
    public ResponseEntity<Long> create(@CurrentMember Member me,
                                       @RequestBody AnniversaryRequest body) {
        return ResponseEntity.ok(anniversaryService.create(me.getId(), body));
    }

    @GetMapping
    public ResponseEntity<List<AnniversaryResponse>> getAll(@CurrentMember Member me) {
        return ResponseEntity.ok(anniversaryService.getAll(me.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(@PathVariable Long id,
                                         @RequestBody AnniversaryRequest request,
                                         @CurrentMember Member me) {
        anniversaryService.update(id, request, me.getId());
        return ResponseEntity.ok("수정 완료");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id,
                                          @CurrentMember Member me) {
        anniversaryService.delete(id, me.getId());
        return ResponseEntity.ok("삭제 완료");
    }
}
