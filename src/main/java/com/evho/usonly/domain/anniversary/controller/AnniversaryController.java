package com.evho.usonly.domain.anniversary.controller;

import com.evho.usonly.domain.anniversary.dto.AnniversaryRequest;
import com.evho.usonly.domain.anniversary.dto.AnniversaryResponse;
import com.evho.usonly.domain.anniversary.service.AnniversaryService;
import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.global.annotation.CurrentMember;
import com.evho.usonly.global.common.ApiResponse;
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
    public ResponseEntity<ApiResponse<Long>> create(@CurrentMember Member me,
                                                    @RequestBody AnniversaryRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(anniversaryService.create(me.getId(), body)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AnniversaryResponse>>> getAll(@CurrentMember Member me) {
        return ResponseEntity.ok(ApiResponse.ok(anniversaryService.getAll(me.getId())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable Long id,
                                                    @RequestBody AnniversaryRequest request,
                                                    @CurrentMember Member me) {
        anniversaryService.update(id, request, me.getId());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @CurrentMember Member me) {
        anniversaryService.delete(id, me.getId());
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
