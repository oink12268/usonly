package com.evho.usonly.domain.game.controller;

import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.domain.member.repository.MemberRepository;
import com.evho.usonly.global.exception.CustomException;
import com.evho.usonly.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GameController {

    private final SimpMessagingTemplate messaging;
    private final MemberRepository memberRepository;

    // coupleId → [firstMemberId, secondMemberId?]
    private final ConcurrentHashMap<Long, List<Long>> gomokuSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, List<Long>> pongSessions   = new ConcurrentHashMap<>();

    // ── 오목 ─────────────────────────────────────────────────────────────────────

    @MessageMapping("/game/gomoku")
    public void handleGomoku(@Payload Map<String, Object> payload, Principal principal) {
        Member me = requireCoupledMember(principal);
        Long coupleId  = me.getCouple().getId();
        Long memberId  = me.getId();
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
            // MOVE / RESET / SURRENDER → 그대로 브로드캐스트 (세션 유지: 역할은 재접속까지 보존)
            messaging.convertAndSend(dest, payload);
        }
    }

    // ── Pong ──────────────────────────────────────────────────────────────────────

    @MessageMapping("/game/pong")
    public void handlePong(@Payload Map<String, Object> payload, Principal principal) {
        Member me = requireCoupledMember(principal);
        Long coupleId  = me.getCouple().getId();
        Long memberId  = me.getId();
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
            // PADDLE / BALL / SCORE / RESET → 그대로 브로드캐스트 (세션 유지: 역할은 재접속까지 보존)
            messaging.convertAndSend(dest, payload);
        }
    }

    // coupleId/memberId를 클라이언트 payload에서 그대로 믿지 않고, 인증된 STOMP Principal
    // (CONNECT 시 검증된 Firebase uid)로부터 서버가 직접 유도함. 이걸 안 하면 임의의 로그인
    // 사용자가 payload에 남의 coupleId를 넣어서 그 커플의 게임 세션에 끼어들거나 실시간 진행
    // 상황을 엿보거나 가짜 MOVE/SCORE를 주입할 수 있었음.
    private Member requireCoupledMember(Principal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        Member member = memberRepository.findByProviderId(principal.getName())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_REGISTERED));
        if (member.getCouple() == null) {
            throw new CustomException(ErrorCode.COUPLE_REQUIRED);
        }
        return member;
    }
}
