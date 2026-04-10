package com.evho.usonly.domain.note.dto;

import com.evho.usonly.domain.note.entity.Note;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NoteResponse {

    private Long id;
    private String title;
    private String content;
    private Long parentId;
    private int childCount;
    private Long createdById;
    private Long lastEditedById;
    private String lastEditedByNickname;
    private boolean isPrivate;
    private Long sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public NoteResponse(Note note) {
        this.id = note.getId();
        this.title = note.getTitle();
        this.content = note.getContent();
        this.parentId = note.getParent() != null ? note.getParent().getId() : null;
        this.childCount = note.getChildren().size();
        if (note.getCreatedBy() != null) {
            this.createdById = note.getCreatedBy().getId();
        }
        if (note.getLastEditedBy() != null) {
            this.lastEditedById = note.getLastEditedBy().getId();
            this.lastEditedByNickname = note.getLastEditedBy().getNickname();
        }
        this.isPrivate = note.isPrivate();
        this.sortOrder = note.getSortOrder();
        this.createdAt = note.getCreatedAt();
        this.updatedAt = note.getUpdatedAt();
    }
}
