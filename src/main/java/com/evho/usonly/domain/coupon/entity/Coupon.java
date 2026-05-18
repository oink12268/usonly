package com.evho.usonly.domain.coupon.entity;

import com.evho.usonly.domain.couple.entity.Couple;
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
@Table(name = "coupon")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "couple_id")
    private Couple couple;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "store_name")
    private String storeName;

    @Column(name = "discount")
    private String discount;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Builder.Default
    @Column(nullable = false)
    private boolean expired = false;

    @Column(name = "chat_id")
    private Long chatId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public void update(String storeName, String discount, LocalDate expiryDate) {
        this.storeName = storeName;
        this.discount = discount;
        this.expiryDate = expiryDate;
    }

    public void markExpired() {
        this.expired = true;
    }
}
