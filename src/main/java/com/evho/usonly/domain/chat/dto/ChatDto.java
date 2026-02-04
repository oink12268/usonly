package com.evho.usonly.domain.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatDto {

    private String message;
    private String writerUid;
    private String sendTime;
}