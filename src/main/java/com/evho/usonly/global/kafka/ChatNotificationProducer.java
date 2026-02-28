package com.evho.usonly.global.kafka;

import com.evho.usonly.global.kafka.event.ChatNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatNotificationProducer {

    private static final String TOPIC = "chat-notification";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(ChatNotificationEvent event) {
        kafkaTemplate.send(TOPIC, event);
    }
}
