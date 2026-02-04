package com.evho.usonly.domain.archive.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Archive {

    private String id;           // 게시글 고유 ID
    private String coupleId;     // 어느 커플의 글인지 (중요!)
    private String writerUid;    // 누가 썼는지

    private String title;        // 제목 (예: "제주도 여행 1일차")
    private String content;      // 내용
    private String photoUrl;     // 업로드된 사진 주소 (Storage URL)

    private String visitDate;    // 방문 날짜 (2024-01-27)
    private String createdAt;    // 작성 시간
}