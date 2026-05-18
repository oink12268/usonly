package com.evho.usonly.domain.coupon.dto;

import com.evho.usonly.domain.coupon.entity.Coupon;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CouponResponse {
    private Long id;
    private String imageUrl;
    private String storeName;
    private String discount;
    private LocalDate expiryDate;
    private boolean expired;
    private Long chatId;
    private LocalDateTime createdAt;

    public static CouponResponse from(Coupon c) {
        return new CouponResponse(
                c.getId(),
                c.getImageUrl(),
                c.getStoreName(),
                c.getDiscount(),
                c.getExpiryDate(),
                c.isExpired(),
                c.getChatId(),
                c.getCreatedAt()
        );
    }
}
