package com.evho.usonly.global.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatNotificationEvent {
    private String writerUid;
    private String message;
}
