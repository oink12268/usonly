package com.evho.usonly.global.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 1. 소켓 연결 주소: ws://localhost:8080/ws
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins);
        // .withSockJS(); // 플러터는 SockJS 안 씀 (순수 소켓 사용)
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 2. 메시지 구독 요청: /sub 채널
        registry.enableSimpleBroker("/sub");
        // 3. 메시지 발행 요청: /pub 채널
        registry.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                // CONNECT 프레임에서만 토큰 검증
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        throw new MessagingException("Authorization header missing");
                    }

                    try {
                        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(authHeader.substring(7));
                        // 검증된 uid를 Principal로 저장 → @MessageMapping 핸들러에서 사용 가능
                        String uid = decodedToken.getUid();
                        accessor.setUser(() -> uid);
                    } catch (Exception e) {
                        throw new MessagingException("Invalid or expired Firebase token");
                    }
                }

                return message;
            }
        });
    }
}
