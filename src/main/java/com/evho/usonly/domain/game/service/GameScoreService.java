package com.evho.usonly.domain.game.service;

import com.evho.usonly.domain.game.dto.GameScoreEntryResponse;
import com.evho.usonly.domain.game.dto.GameScoreRequest;
import com.evho.usonly.domain.game.dto.GameScoreSummaryResponse;
import com.evho.usonly.domain.game.entity.GameScore;
import com.evho.usonly.domain.game.repository.GameScoreRepository;
import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameScoreService {

    private final GameScoreRepository gameScoreRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public GameScoreSummaryResponse submitScore(String firebaseUid, GameScoreRequest req) {
        Member me = memberRepository.findByProviderId(firebaseUid)
                .orElseThrow(() -> new IllegalStateException("회원을 찾을 수 없습니다."));

        gameScoreRepository.save(GameScore.builder()
                .member(me)
                .gameType(req.getGameType())
                .score(req.getScore())
                .build());

        int myBest = gameScoreRepository.findBestScore(me.getId(), req.getGameType())
                .orElse(req.getScore());

        if (me.getCouple() == null) {
            return new GameScoreSummaryResponse(myBest, null, null, null);
        }

        Member partner = memberRepository.findAllByCoupleId(me.getCouple().getId())
                .stream().filter(m -> !m.getId().equals(me.getId())).findFirst().orElse(null);

        Integer partnerBest = partner != null
                ? gameScoreRepository.findBestScore(partner.getId(), req.getGameType()).orElse(null)
                : null;

        return new GameScoreSummaryResponse(
                myBest,
                partnerBest,
                partner != null ? partner.getNickname() : null,
                partner != null ? partner.getProfileImageUrl() : null
        );
    }

    @Transactional(readOnly = true)
    public List<GameScoreEntryResponse> getTop10(String firebaseUid, String gameType) {
        Member me = memberRepository.findByProviderId(firebaseUid)
                .orElseThrow(() -> new IllegalStateException("회원을 찾을 수 없습니다."));

        if (me.getCouple() == null) return List.of();

        List<GameScore> scores = gameScoreRepository
                .findTop10ByMember_Couple_IdAndGameTypeOrderByScoreDescPlayedAtAsc(
                        me.getCouple().getId(), gameType);

        List<GameScoreEntryResponse> result = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            GameScore gs = scores.get(i);
            result.add(GameScoreEntryResponse.of(i + 1, gs, gs.getMember().getId().equals(me.getId())));
        }
        return result;
    }
}
