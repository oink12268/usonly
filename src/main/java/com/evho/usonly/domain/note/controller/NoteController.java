package com.evho.usonly.domain.note.controller;

import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.domain.note.dto.NoteEvent;
import com.evho.usonly.domain.note.dto.NoteMoveRequest;
import com.evho.usonly.domain.note.dto.NoteReorderRequest;
import com.evho.usonly.domain.note.dto.NoteRequest;
import com.evho.usonly.domain.note.dto.NoteResponse;
import com.evho.usonly.domain.note.service.NoteService;
import com.evho.usonly.domain.note.service.NoteScheduleExtractService;
import com.evho.usonly.global.annotation.CurrentMember;
import com.evho.usonly.global.common.ApiResponse;
import com.evho.usonly.global.utils.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    private final NoteScheduleExtractService noteScheduleExtractService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${custom.file.dir}")
    private String uploadDir;

    @Value("${custom.file.domain}")
    private String baseUrl;

    @PostMapping("/extract-schedule")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> extractSchedule(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "content가 비어있습니다."));
        }
        List<Map<String, String>> events = noteScheduleExtractService.extractSchedules(content);
        if (events.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(List.of()));
        }
        return ResponseEntity.ok(ApiResponse.ok(events));
    }

    @PostMapping("/image")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadNoteImage(@RequestParam("file") MultipartFile file) throws IOException {
        String fileName = FileUploadUtil.generateSafeFilename(file, FileUploadUtil.imageExtensions());
        File dest = new File(uploadDir + "notes/" + fileName);
        if (!dest.getParentFile().exists()) dest.getParentFile().mkdirs();
        file.transferTo(dest);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("imageUrl", baseUrl + "notes/" + fileName)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NoteResponse>>> getNotes(@CurrentMember Member member,
                                                                    @RequestParam(required = false) Long parentId) {
        if (member.getCouple() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "커플이 연결되지 않았습니다."));
        }
        return ResponseEntity.ok(ApiResponse.ok(
                noteService.getNotes(member.getCouple().getId(), parentId, member.getId())
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NoteResponse>> createNote(@CurrentMember Member member,
                                                                @RequestBody NoteRequest dto) {
        NoteResponse response = noteService.createNote(dto, member);
        if (!response.isPrivate()) {
            messagingTemplate.convertAndSend(
                    "/sub/couple/" + member.getCouple().getId() + "/notes",
                    new NoteEvent("CREATED", response, null)
            );
        }
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NoteResponse>> updateNote(@PathVariable Long id,
                                                                @CurrentMember Member member,
                                                                @RequestBody NoteRequest dto) {
        NoteResponse response = noteService.updateNote(id, dto, member);
        if (!response.isPrivate()) {
            messagingTemplate.convertAndSend(
                    "/sub/couple/" + member.getCouple().getId() + "/notes",
                    new NoteEvent("UPDATED", response, null)
            );
        }
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/reorder")
    public ResponseEntity<Void> reorderNotes(@CurrentMember Member member,
                                             @RequestBody NoteReorderRequest dto) {
        noteService.reorderNotes(dto, member);
        messagingTemplate.convertAndSend(
                "/sub/couple/" + member.getCouple().getId() + "/notes",
                new NoteEvent("REORDERED", null, null, null, dto.getOrderedIds(), dto.getParentId())
        );
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/move")
    public ResponseEntity<ApiResponse<NoteResponse>> moveNote(@PathVariable Long id,
                                                              @CurrentMember Member member,
                                                              @RequestBody NoteMoveRequest body) {
        NoteResponse response = noteService.moveNote(id, body.getTargetParentId(), member);
        messagingTemplate.convertAndSend(
                "/sub/couple/" + member.getCouple().getId() + "/notes",
                new NoteEvent("MOVED", response, null, id)
        );
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id,
                                           @CurrentMember Member member) {
        Long coupleId = member.getCouple().getId();
        noteService.deleteNote(id, member);
        messagingTemplate.convertAndSend(
                "/sub/couple/" + coupleId + "/notes",
                new NoteEvent("DELETED", null, id)
        );
        return ResponseEntity.noContent().build();
    }
}
