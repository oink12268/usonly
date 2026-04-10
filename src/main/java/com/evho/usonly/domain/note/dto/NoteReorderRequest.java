package com.evho.usonly.domain.note.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NoteReorderRequest {
    private List<Long> orderedIds;
    private Long parentId; // null = 최상위
}
