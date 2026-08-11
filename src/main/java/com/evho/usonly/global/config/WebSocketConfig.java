package com.evho.usonly.global.config;

import com.evho.usonly.domain.member.dto.MemberCacheDto;
import com.evho.usonly.domain.member.service.MemberService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.ObjectProvider;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // 구독 가능한 목적지 prefix. 뒤에 coupleId가 붙는다. 더 긴 prefix부터 검사해야
    // "/sub/chat/typing/42"가 "/sub/chat/" + "typing/42"로 잘리지 않는다.
    private static final List<String> CHAT_TOPIC_PREFIXES = List.of(
            "/sub/chat/typing/",
            "/sub/chat/edit/",
            "/sub/chat/delete/",
            "/sub/chat/"
    );

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    // WebSocket 설정은 애플리케이션 초기화 초반에 만들어지므로 MemberService를 지연 주입한다.
    private final ObjectProvider<MemberService> memberServiceProvider;

    public WebSocketConfig(ObjectProvider<MemberService> memberServiceProvider) {
        this.memberServiceProvider = memberServiceProvider;
    }

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
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("ws-heartbeat-");
        taskScheduler.initialize();

        registry.enableSimpleBroker("/sub")
                .setHeartbeatValue(new long[]{25000, 25000})  // 25초 heartbeat
                .setTaskScheduler(taskScheduler);
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

                // SUBSCRIBE 프레임은 목적지의 coupleId가 인증된 사용자의 커플과 일치하는지 검증.
                // 없으면 인증만 통과한 아무 사용자나 /sub/chat/{남의coupleId}를 구독해 실시간 대화를 볼 수 있다.
                if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    verifySubscription(accessor);
                }

                return message;
            }
        });
    }

    private void verifySubscription(StompHeaderAccessor accessor) {
        Long topicCoupleId = resolveTopicCoupleId(accessor.getDestination());
        if (topicCoupleId == null) {
            throw new MessagingException("Unknown subscription destination");
        }

        Principal user = accessor.getUser();
        if (user == null) {
            throw new MessagingException("Unauthenticated subscription");
        }

        MemberCacheDto member = memberServiceProvider.getObject().findByProviderId(user.getName());
        if (member == null || member.getCoupleId() == null || !member.getCoupleId().equals(topicCoupleId)) {
            throw new MessagingException("Forbidden subscription");
        }
    }

    // 허용된 목적지면 뒤에 붙은 coupleId를, 아니면 null을 반환(= 구독 거부)
    private Long resolveTopicCoupleId(String destination) {
        if (destination == null) return null;
        for (String prefix : CHAT_TOPIC_PREFIXES) {
            if (!destination.startsWith(prefix)) continue;
            try {
                return Long.parseLong(destination.substring(prefix.length()));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
