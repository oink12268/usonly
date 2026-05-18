package com.evho.usonly.domain.notification.entity;

import com.evho.usonly.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "notification_setting")
public class NotificationSetting {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Builder.Default
    @Column(nullable = false)
    private boolean calendarEnabled = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean chatEnabled = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean anniversaryEnabled = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean couponEnabled = true;

    // 다음날 일정 알림 시각 (0~23, 기본값 22시)
    @Builder.Default
    @Column(nullable = false)
    private int calendarReminderHour = 22;
}
