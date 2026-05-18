package com.evho.usonly.domain.coupon.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class CouponRequest {
    private String storeName;
    private String discount;
    private LocalDate expiryDate;
}
