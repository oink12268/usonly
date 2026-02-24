package com.evho.usonly.domain.anniversary.model;

import com.evho.usonly.domain.couple.model.Couple;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "anniversary")
public class Anniversary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "is_recurring")
    private boolean recurring; // 매년 반복 여부

    @Column(name = "is_lunar")
    private boolean lunar; // 음력 여부

    @Column(name = "lunar_month")
    private Integer lunarMonth; // 음력 월 (1-12)

    @Column(name = "lunar_day")
    private Integer lunarDay; // 음력 일 (1-30)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "couple_id")
    private Couple couple;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public void update(String title, LocalDate date, boolean recurring,
                       boolean lunar, Integer lunarMonth, Integer lunarDay) {
        this.title = title;
        this.date = date;
        this.recurring = recurring;
        this.lunar = lunar;
        this.lunarMonth = lunarMonth;
        this.lunarDay = lunarDay;
    }
}
