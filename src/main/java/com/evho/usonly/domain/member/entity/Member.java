package com.evho.usonly.domain.member.entity;

import com.evho.usonly.domain.couple.entity.Couple;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "member")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Member {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String nickname;

    private String profileImageUrl;

    private String provider;    // "KAKAO", "GOOGLE"
    private String providerId;  // 소셜 서비스의 고유 ID
    private String invitationCode;

    @Enumerated(EnumType.STRING)
    private Role role;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "couple_id")
    private Couple couple;

    @Column(name = "fcm_token")
    private String fcmToken;

    public void updateFcmToken(String token) {
        this.fcmToken = token;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateProfileImageUrl(String url) {
        this.profileImageUrl = url;
    }

    public void connectCouple(Couple couple) {
        this.couple = couple;
    }
    public enum Role {
        USER, ADMIN
    }
}