package com.evho.usonly.global.redis;

import com.evho.usonly.domain.chat.entity.Chat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = buildObjectMapper();

    private static ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());

        try {
            switch (channel) {
                case "chat:message" -> {
                    Chat chat = objectMapper.readValue(body, Chat.class);
                    messagingTemplate.convertAndSend("/sub/chat", chat);
                }
                case "chat:typing" -> {
                    Map<?, ?> payload = objectMapper.readValue(body, Map.class);
                    messagingTemplate.convertAndSend("/sub/chat/typing", payload);
                }
                case "chat:delete" -> {
                    Map<?, ?> payload = objectMapper.readValue(body, Map.class);
                    messagingTemplate.convertAndSend("/sub/chat/delete", payload);
                }
                default -> log.warn("Unknown Redis channel: {}", channel);
            }
        } catch (Exception e) {
            log.error("Redis message processing error. channel={}, body={}", channel, body, e);
        }
    }
}
