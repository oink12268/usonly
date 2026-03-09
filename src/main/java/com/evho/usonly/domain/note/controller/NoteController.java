package com.evho.usonly.domain.note.controller;

import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.domain.note.dto.NoteEvent;
import com.evho.usonly.domain.note.dto.NoteRequest;
import com.evho.usonly.domain.note.dto.NoteResponse;
import com.evho.usonly.domain.note.service.NoteService;
import com.evho.usonly.domain.note.service.NoteScheduleExtractService;
import com.evho.usonly.global.annotation.CurrentMember;
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
    public ResponseEntity<Map<String, String>> extractSchedule(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        Map<String, String> result = noteScheduleExtractService.extractSchedule(content);
        if (result == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/image")
    public Map<String, String> uploadNoteImage(@RequestParam("file") MultipartFile file) throws IOException {
        String fileName = FileUploadUtil.generateSafeFilename(file, FileUploadUtil.imageExtensions());
        File dest = new File(uploadDir + fileName);
        if (!dest.getParentFile().exists()) dest.getParentFile().mkdirs();
        file.transferTo(dest);
        return Map.of("imageUrl", baseUrl + fileName);
    }

    @GetMapping
    public ResponseEntity<List<NoteResponse>> getNotes(@CurrentMember Member member,
                                                       @RequestParam(required = false) Long parentId) {
        if (member.getCouple() == null) {
            return ResponseEntity.badRequest().build();
        }
        List<NoteResponse> notes = noteService.getNotes(member.getCouple().getId(), parentId);
        return ResponseEntity.ok(notes);
    }

    @PostMapping
    public ResponseEntity<NoteResponse> createNote(@CurrentMember Member member,
                                                      @RequestBody NoteRequest dto) {
        NoteResponse response = noteService.createNote(dto, member);

        messagingTemplate.convertAndSend(
                "/sub/couple/" + member.getCouple().getId() + "/notes",
                new NoteEvent("CREATED", response, null)
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteResponse> updateNote(@PathVariable Long id,
                                                      @CurrentMember Member member,
                                                      @RequestBody NoteRequest dto) {
        NoteResponse response = noteService.updateNote(id, dto, member);

        messagingTemplate.convertAndSend(
                "/sub/couple/" + member.getCouple().getId() + "/notes",
                new NoteEvent("UPDATED", response, null)
        );

        return ResponseEntity.ok(response);
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
