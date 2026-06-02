package com.evho.usonly.domain.game.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GameController {

    private final SimpMessagingTemplate messaging;

    // coupleId → [firstMemberId, secondMemberId?]
    private final ConcurrentHashMap<Long, List<Long>> gomokuSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, List<Long>> pongSessions   = new ConcurrentHashMap<>();

    // ── 오목 ─────────────────────────────────────────────────────────────────────

    @MessageMapping("/game/gomoku")
    public void handleGomoku(@Payload Map<String, Object> payload) {
        Long coupleId  = toLong(payload.get("coupleId"));
        Long memberId  = toLong(payload.get("memberId"));
        String type    = (String) payload.get("type");
        String dest    = "/sub/couple/" + coupleId + "/game/gomoku";

        if ("JOIN".equals(type)) {
            List<Long> session = gomokuSessions.computeIfAbsent(coupleId, k -> new ArrayList<>());
            synchronized (session) {
                if (!session.contains(memberId)) session.add(memberId);

                // 두 플레이어 모두 접속 → 역할 배정
                if (session.size() >= 2) {
                    long blackId = session.get(0);
                    long whiteId = session.get(1);
                    messaging.convertAndSend(dest, Map.of("type", "ROLE", "memberId", blackId, "isBlack", true));
                    messaging.convertAndSend(dest, Map.of("type", "ROLE", "memberId", whiteId, "isBlack", false));
                } else {
                    // 첫 번째 플레이어: JOIN 에코만
                    messaging.convertAndSend(dest, payload);
                }
            }
        } else {
            // MOVE / RESET / SURRENDER → 그대로 브로드캐스트
            if ("RESET".equals(type)) gomokuSessions.remove(coupleId);
            messaging.convertAndSend(dest, payload);
        }
    }

    // ── Pong ──────────────────────────────────────────────────────────────────────

    @MessageMapping("/game/pong")
    public void handlePong(@Payload Map<String, Object> payload) {
        Long coupleId  = toLong(payload.get("coupleId"));
        Long memberId  = toLong(payload.get("memberId"));
        String type    = (String) payload.get("type");
        String dest    = "/sub/couple/" + coupleId + "/game/pong";

        if ("JOIN".equals(type)) {
            List<Long> session = pongSessions.computeIfAbsent(coupleId, k -> new ArrayList<>());
            synchronized (session) {
                if (!session.contains(memberId)) session.add(memberId);

                // 두 플레이어 모두 접속 → 역할 배정
                if (session.size() >= 2) {
                    long leftId  = session.get(0);
                    long rightId = session.get(1);
                    messaging.convertAndSend(dest, Map.of("type", "ROLE", "memberId", leftId,  "isLeft", true));
                    messaging.convertAndSend(dest, Map.of("type", "ROLE", "memberId", rightId, "isLeft", false));
                } else {
                    messaging.convertAndSend(dest, payload);
                }
            }
        } else {
            // PADDLE / BALL / SCORE / RESET → 그대로 브로드캐스트
            if ("RESET".equals(type)) pongSessions.remove(coupleId);
            messaging.convertAndSend(dest, payload);
        }
    }

    private Long toLong(Object val) {
        if (val == null) return 0L;
        return ((Number) val).longValue();
    }
}
