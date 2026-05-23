package com.evho.usonly.domain.game.entity;

import com.evho.usonly.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "game_score", indexes = {
        @Index(name = "idx_game_score_couple_type", columnList = "member_id, game_type")
})
public class GameScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "game_type", nullable = false, length = 20)
    private String gameType;

    @Column(nullable = false)
    private int score;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime playedAt;
}
