package com.evho.usonly.domain.note.dto;

import com.evho.usonly.domain.note.model.Note;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NoteResponseDto {

    private Long id;
    private String title;
    private String content;
    private Long lastEditedById;
    private String lastEditedByNickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public NoteResponseDto(Note note) {
        this.id = note.getId();
        this.title = note.getTitle();
        this.content = note.getContent();
        if (note.getLastEditedBy() != null) {
            this.lastEditedById = note.getLastEditedBy().getId();
            this.lastEditedByNickname = note.getLastEditedBy().getNickname();
        }
        this.createdAt = note.getCreatedAt();
        this.updatedAt = note.getUpdatedAt();
    }
}
