package com.evho.usonly.domain.archive.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ArchiveCreateRequest {
    private String writerUid; // 작성자 UID
    private String title;     // 제목
    private String content;   // 내용
    private String visitDate; // 방문 날짜 (2024-01-27)
}