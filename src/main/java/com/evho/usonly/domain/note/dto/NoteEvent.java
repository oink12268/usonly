package com.evho.usonly.domain.note.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NoteEvent {
    private String type;      // "CREATED", "UPDATED", "DELETED", "MOVED"
    private NoteResponse note; // DELETED 시 null
    private Long deletedId;   // DELETED 시에만 사용
    private Long movedId;     // MOVED 시에만 사용 (목록에서 제거할 noteId)

    public NoteEvent(String type, NoteResponse note, Long deletedId) {
        this(type, note, deletedId, null);
    }
}
