package com.evho.usonly.domain.archive.dto;

import com.evho.usonly.domain.archive.model.Archive;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArchiveResponse {
    private String archiveId;
    private String title;
    private String content;
    private String photoUrl;
    private String visitDate;  // 방문 날짜
    private String writerUid;  // 누가 썼는지

    // Archive 객체를 받아서 Response로 변환하는 '생성자' 같은 메소드
    public static ArchiveResponse from(Archive archive) {
        return ArchiveResponse.builder()
                .archiveId(archive.getId())
                .title(archive.getTitle())
                .content(archive.getContent())
                .photoUrl(archive.getPhotoUrl())
                .visitDate(archive.getVisitDate())
                .writerUid(archive.getWriterUid())
                .build();
    }
}