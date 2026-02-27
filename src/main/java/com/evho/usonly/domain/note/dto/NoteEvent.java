package com.evho.usonly.domain.note.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NoteEvent {
    private String type;      // "CREATED", "UPDATED", "DELETED"
    private NoteResponse note; // DELETED 시 null
    private Long deletedId;   // DELETED 시에만 사용
}
