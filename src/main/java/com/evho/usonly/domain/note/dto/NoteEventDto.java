package com.evho.usonly.domain.note.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NoteEventDto {
    private String type;          // "CREATED", "UPDATED", "DELETED"
    private NoteResponseDto note; // DELETED 시 null
    private Long deletedId;       // DELETED 시에만 사용
}
