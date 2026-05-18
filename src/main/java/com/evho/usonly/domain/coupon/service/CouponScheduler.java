package com.evho.usonly.domain.coupon.service;

import com.evho.usonly.domain.coupon.entity.Coupon;
import com.evho.usonly.domain.coupon.repository.CouponRepository;
import com.evho.usonly.domain.member.repository.MemberRepository;
import com.evho.usonly.domain.notification.service.NotificationSettingService;
import com.evho.usonly.global.fcm.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class CouponScheduler {

    private final CouponRepository couponRepository;
    private final MemberRepository memberRepository;
    private final CouponService couponService;
    private final FcmService fcmService;
    private final NotificationSettingService notificationSettingService;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void sendCouponExpiryReminders() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        // 만료 아카이브
        couponService.archiveExpired();

        // D-7, D-1 알림
        for (int daysLeft : new int[]{7, 1}) {
            LocalDate target = today.plusDays(daysLeft);
            String when = daysLeft == 7 ? "일주일 후" : "내일";

            couponRepository.findExpiringOn(target).forEach(coupon -> {
                String title = coupon.getStoreName() != null ? coupon.getStoreName() : "쿠폰";
                String body = "[" + title + "] 쿠폰이 " + when + " 만료돼요!";

                memberRepository.findAllByCoupleId(coupon.getCouple().getId())
                        .stream()
                        .filter(m -> m.getFcmToken() != null)
                        .filter(m -> notificationSettingService.isCouponEnabled(m.getId()))
                        .forEach(m -> fcmService.sendCouponPush(m.getFcmToken(), "쿠폰 만료 알림", body));
            });
        }
    }
}
