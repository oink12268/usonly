package com.evho.usonly.domain.coupon.repository;

import com.evho.usonly.domain.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    List<Coupon> findByCoupleIdOrderByCreatedAtDesc(Long coupleId);

    // D-7 또는 D-1 알림 대상 (만료 안 된 것 중 만료일이 정확히 N일 후인 것)
    @Query("SELECT c FROM Coupon c WHERE c.expired = false AND c.expiryDate = :targetDate")
    List<Coupon> findExpiringOn(@Param("targetDate") LocalDate targetDate);

    // 만료 처리 대상 (만료일이 오늘 이전이고 아직 expired = false인 것)
    @Query("SELECT c FROM Coupon c WHERE c.expired = false AND c.expiryDate IS NOT NULL AND c.expiryDate < :today")
    List<Coupon> findExpiredCoupons(@Param("today") LocalDate today);

    boolean existsByCoupleIdAndImageUrl(Long coupleId, String imageUrl);
}
