package com.evho.usonly.domain.game.repository;

import com.evho.usonly.domain.game.entity.GameScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameScoreRepository extends JpaRepository<GameScore, Long> {

    // 커플 내 상위 10개 기록 (두 멤버 합산)
    List<GameScore> findTop10ByMember_Couple_IdAndGameTypeOrderByScoreDescPlayedAtAsc(
            Long coupleId, String gameType);

    // 특정 멤버의 최고 점수
    @Query("SELECT MAX(gs.score) FROM GameScore gs WHERE gs.member.id = :memberId AND gs.gameType = :gameType")
    Optional<Integer> findBestScore(@Param("memberId") Long memberId, @Param("gameType") String gameType);

    void deleteAllByMemberId(Long memberId);
}
