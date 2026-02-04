package com.evho.usonly.domain.archive.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ArchiveWriteRequest {
    private String title;
    private String content;
    private String writerUid;
    private String visitDate;
}