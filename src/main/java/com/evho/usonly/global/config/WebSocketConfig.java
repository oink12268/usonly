package com.evho.usonly.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 1. 소켓 연결 주소: ws://localhost:8080/ws
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*"); // 모든 출처 허용
        // .withSockJS(); // 플러터는 SockJS 안 씀 (순수 소켓 사용)
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 2. 메시지 구독 요청: /sub 채널
        registry.enableSimpleBroker("/sub");
        // 3. 메시지 발행 요청: /pub 채널
        registry.setApplicationDestinationPrefixes("/pub");
    }
}
