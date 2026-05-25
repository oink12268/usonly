package com.evho.usonly.domain.coupon.controller;

import com.evho.usonly.domain.coupon.dto.CouponRequest;
import com.evho.usonly.domain.coupon.dto.CouponResponse;
import com.evho.usonly.domain.coupon.service.CouponService;
import com.evho.usonly.domain.member.dto.MemberCacheDto;
import com.evho.usonly.domain.member.service.MemberService;
import com.evho.usonly.global.common.ApiResponse;
import com.evho.usonly.global.exception.CustomException;
import com.evho.usonly.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;
    private final MemberService memberService;

    @GetMapping
    public ApiResponse<List<CouponResponse>> getCoupons(
            @RequestAttribute("firebaseUid") String firebaseUid) {
        Long coupleId = getCoupleId(firebaseUid);
        if (coupleId == null) return ApiResponse.ok(List.of());
        return ApiResponse.ok(couponService.findByCoupleId(coupleId));
    }

    @PutMapping("/{id}")
    public ApiResponse<CouponResponse> updateCoupon(
            @PathVariable Long id,
            @RequestBody CouponRequest req,
            @RequestAttribute("firebaseUid") String firebaseUid) {
        Long coupleId = getCoupleId(firebaseUid);
        if (coupleId == null) throw new CustomException(ErrorCode.COUPLE_REQUIRED);
        return ApiResponse.ok(couponService.update(id, req, coupleId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCoupon(
            @PathVariable Long id,
            @RequestAttribute("firebaseUid") String firebaseUid) {
        Long coupleId = getCoupleId(firebaseUid);
        if (coupleId == null) throw new CustomException(ErrorCode.COUPLE_REQUIRED);
        couponService.delete(id, coupleId);
        return ApiResponse.ok();
    }

    private Long getCoupleId(String firebaseUid) {
        MemberCacheDto member = memberService.findByProviderId(firebaseUid);
        return member != null ? member.getCoupleId() : null;
    }
}
