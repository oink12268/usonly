package com.evho.usonly.domain.anniversary.service;

import com.evho.usonly.domain.anniversary.entity.Anniversary;
import com.evho.usonly.domain.anniversary.repository.AnniversaryRepository;
import com.evho.usonly.domain.member.repository.MemberRepository;
import com.evho.usonly.global.fcm.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class AnniversaryScheduler {

    private final AnniversaryRepository anniversaryRepository;
    private final MemberRepository memberRepository;
    private final AnniversaryService anniversaryService;
    private final FcmService fcmService;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void sendAnniversaryReminders() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        anniversaryRepository.findAll().forEach(ann -> {
            LocalDate next = anniversaryService.getNextOccurrence(ann, today);
            if (next == null) return;

            long days = ChronoUnit.DAYS.between(today, next);
            if (days != 7 && days != 1) return;

            String when = days == 7 ? "일주일 후" : "내일";
            String body = when + "이 [" + ann.getTitle() + "] 기념일이에요 \uD83D\uDC95";

            memberRepository.findAllByCoupleId(ann.getCouple().getId())
                    .stream()
                    .filter(m -> m.getFcmToken() != null)
                    .forEach(m -> fcmService.sendAnniversaryPush(m.getFcmToken(), "기념일 알림", body));
        });
    }
}
