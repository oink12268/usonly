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
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        return mapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());

        // 채널 형식: chat:{type}:{coupleId} — 커플별 STOMP 토픽으로만 전달해서 다른 커플에게 안 새게 함
        String[] parts = channel.split(":", 3);
        if (parts.length != 3) {
            log.warn("Unexpected Redis channel format: {}", channel);
            return;
        }
        String type = parts[1];
        String coupleId = parts[2];

        try {
            switch (type) {
                case "message" -> {
                    Chat chat = objectMapper.readValue(body, Chat.class);
                    messagingTemplate.convertAndSend("/sub/chat/" + coupleId, chat);
                }
                case "typing" -> {
                    Map<?, ?> payload = objectMapper.readValue(body, Map.class);
                    messagingTemplate.convertAndSend("/sub/chat/typing/" + coupleId, payload);
                }
                case "delete" -> {
                    Map<?, ?> payload = objectMapper.readValue(body, Map.class);
                    messagingTemplate.convertAndSend("/sub/chat/delete/" + coupleId, payload);
                }
                case "edit" -> {
                    Map<?, ?> payload = objectMapper.readValue(body, Map.class);
                    messagingTemplate.convertAndSend("/sub/chat/edit/" + coupleId, payload);
                }
                default -> log.warn("Unknown Redis chat type: {}", type);
            }
        } catch (Exception e) {
            log.error("Redis message processing error. channel={}, body={}", channel, body, e);
        }
    }
}
