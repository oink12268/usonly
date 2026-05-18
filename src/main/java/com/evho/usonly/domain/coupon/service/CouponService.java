package com.evho.usonly.domain.coupon.service;

import com.evho.usonly.domain.coupon.dto.CouponRequest;
import com.evho.usonly.domain.coupon.dto.CouponResponse;
import com.evho.usonly.domain.coupon.entity.Coupon;
import com.evho.usonly.domain.coupon.repository.CouponRepository;
import com.evho.usonly.domain.couple.entity.Couple;
import com.evho.usonly.domain.couple.repository.CoupleRepository;
import com.evho.usonly.global.gemini.GeminiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CoupleRepository coupleRepository;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    private static final String COUPON_DETECT_PROMPT =
            "이 이미지가 쿠폰 또는 할인권인지 분석해줘. " +
            "JSON으로만 답해:\n" +
            "{\n" +
            "  \"isCoupon\": true/false,\n" +
            "  \"storeName\": \"브랜드명 또는 가게명 (없으면 null)\",\n" +
            "  \"discount\": \"할인 내용 요약 (예: 5000원 할인, 30% 할인, 없으면 null)\",\n" +
            "  \"expiryDate\": \"만료일 yyyy-MM-dd 형식 (없거나 모르면 null)\"\n" +
            "}";

    @Async
    public void detectAndSave(String imageUrl, Long coupleId, Long chatId) {
        if (coupleId == null) return;
        if (couponRepository.existsByCoupleIdAndImageUrl(coupleId, imageUrl)) return;

        try {
            String json = geminiClient.generateJsonWithImage(COUPON_DETECT_PROMPT, imageUrl);
            Map<?, ?> result = objectMapper.readValue(json, Map.class);

            Boolean isCoupon = (Boolean) result.get("isCoupon");
            if (!Boolean.TRUE.equals(isCoupon)) return;

            String storeName = (String) result.get("storeName");
            String discount = (String) result.get("discount");
            String expiryStr = (String) result.get("expiryDate");
            LocalDate expiryDate = null;
            if (expiryStr != null && !expiryStr.isBlank()) {
                try { expiryDate = LocalDate.parse(expiryStr); } catch (Exception ignored) {}
            }

            Couple couple = coupleRepository.findById(coupleId).orElse(null);
            if (couple == null) return;

            couponRepository.save(Coupon.builder()
                    .couple(couple)
                    .imageUrl(imageUrl)
                    .storeName(storeName)
                    .discount(discount)
                    .expiryDate(expiryDate)
                    .chatId(chatId)
                    .build());

            log.info("쿠폰 감지 저장 완료: coupleId={}, store={}", coupleId, storeName);
        } catch (Exception e) {
            log.warn("쿠폰 감지 실패: imageUrl={}, error={}", imageUrl, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> findByCoupleId(Long coupleId) {
        return couponRepository.findByCoupleIdOrderByCreatedAtDesc(coupleId)
                .stream().map(CouponResponse::from).toList();
    }

    @Transactional
    public CouponResponse update(Long id, CouponRequest req, Long coupleId) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));
        if (!coupon.getCouple().getId().equals(coupleId)) {
            throw new IllegalStateException("접근 권한이 없습니다.");
        }
        coupon.update(req.getStoreName(), req.getDiscount(), req.getExpiryDate());
        return CouponResponse.from(coupon);
    }

    @Transactional
    public void delete(Long id, Long coupleId) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));
        if (!coupon.getCouple().getId().equals(coupleId)) {
            throw new IllegalStateException("접근 권한이 없습니다.");
        }
        couponRepository.deleteById(id);
    }

    @Transactional
    public void archiveExpired() {
        List<Coupon> expired = couponRepository.findExpiredCoupons(LocalDate.now());
        expired.forEach(Coupon::markExpired);
        if (!expired.isEmpty()) {
            log.info("만료 쿠폰 아카이브 처리: {}건", expired.size());
        }
    }
}
