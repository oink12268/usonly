package com.evho.usonly.domain.note.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoteMoveRequest {
    private Long targetParentId; // null이면 최상위로 이동
}
