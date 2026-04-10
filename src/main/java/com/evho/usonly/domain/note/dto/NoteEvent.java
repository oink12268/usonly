package com.evho.usonly.domain.note.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class NoteEvent {
    private String type;           // "CREATED", "UPDATED", "DELETED", "MOVED", "REORDERED"
    private NoteResponse note;     // DELETED 시 null
    private Long deletedId;        // DELETED 시에만 사용
    private Long movedId;          // MOVED 시에만 사용
    private List<Long> orderedIds; // REORDERED 시에만 사용
    private Long reorderParentId;  // REORDERED 시에만 사용 (null = 최상위)

    public NoteEvent(String type, NoteResponse note, Long deletedId) {
        this(type, note, deletedId, null, null, null);
    }

    public NoteEvent(String type, NoteResponse note, Long deletedId, Long movedId) {
        this(type, note, deletedId, movedId, null, null);
    }
}
