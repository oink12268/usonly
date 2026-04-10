package com.evho.usonly.domain.note.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoteRequest {
    private String title;
    private String content;
    private Long parentId;
    private boolean isPrivate;
}
