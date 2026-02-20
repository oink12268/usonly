package com.evho.usonly.domain.note.controller;

import com.evho.usonly.domain.member.model.Member;
import com.evho.usonly.domain.note.dto.NoteEventDto;
import com.evho.usonly.domain.note.dto.NoteRequestDto;
import com.evho.usonly.domain.note.dto.NoteResponseDto;
import com.evho.usonly.domain.note.model.Note;
import com.evho.usonly.domain.note.service.NoteService;
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
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${custom.file.dir}")
    private String uploadDir;

    @Value("${custom.file.domain}")
    private String baseUrl;

    @PostMapping("/image")
    public Map<String, String> uploadNoteImage(@RequestParam("file") MultipartFile file) throws IOException {
        String fileName = FileUploadUtil.generateSafeFilename(file, FileUploadUtil.imageExtensions());
        File dest = new File(uploadDir + fileName);
        if (!dest.getParentFile().exists()) dest.getParentFile().mkdirs();
        file.transferTo(dest);
        return Map.of("imageUrl", baseUrl + fileName);
    }

    @GetMapping
    public ResponseEntity<List<NoteResponseDto>> getNotes(@CurrentMember Member member) {
        if (member.getCouple() == null) {
            return ResponseEntity.badRequest().build();
        }
        List<NoteResponseDto> notes = noteService.getNotes(member.getCouple().getId())
                .stream()
                .map(NoteResponseDto::new)
                .toList();
        return ResponseEntity.ok(notes);
    }

    @PostMapping
    public ResponseEntity<NoteResponseDto> createNote(@CurrentMember Member member,
                                                      @RequestBody NoteRequestDto dto) {
        Note note = noteService.createNote(dto, member);
        NoteResponseDto response = new NoteResponseDto(note);

        messagingTemplate.convertAndSend(
                "/sub/couple/" + member.getCouple().getId() + "/notes",
                new NoteEventDto("CREATED", response, null)
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteResponseDto> updateNote(@PathVariable Long id,
                                                      @CurrentMember Member member,
                                                      @RequestBody NoteRequestDto dto) {
        Note note = noteService.updateNote(id, dto, member);
        NoteResponseDto response = new NoteResponseDto(note);

        messagingTemplate.convertAndSend(
                "/sub/couple/" + member.getCouple().getId() + "/notes",
                new NoteEventDto("UPDATED", response, null)
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
                new NoteEventDto("DELETED", null, id)
        );

        return ResponseEntity.noContent().build();
    }
}
