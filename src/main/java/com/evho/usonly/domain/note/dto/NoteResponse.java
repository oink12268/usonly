package com.evho.usonly.domain.note.dto;

import com.evho.usonly.domain.note.entity.Note;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NoteResponse {

    private Long id;
    private String title;
    private String content;
    private Long lastEditedById;
    private String lastEditedByNickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public NoteResponse(Note note) {
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
