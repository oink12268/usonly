package com.evho.usonly.domain.schedule.service;

import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.domain.member.repository.MemberRepository;
import com.evho.usonly.domain.notification.entity.NotificationSetting;
import com.evho.usonly.domain.notification.service.NotificationSettingService;
import com.evho.usonly.domain.schedule.entity.Schedule;
import com.evho.usonly.domain.schedule.repository.ScheduleRepository;
import com.evho.usonly.global.fcm.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ScheduleReminderScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final MemberRepository memberRepository;
    private final NotificationSettingService notificationSettingService;
    private final ScheduleRepository scheduleRepository;
    private final FcmService fcmService;

    // 매 정시마다 실행하여 알림 시각이 현재 시각과 일치하는 멤버에게 발송
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void sendTomorrowScheduleReminders() {
        int currentHour = LocalTime.now(KST).getHour();
        LocalDate tomorrow = LocalDate.now(KST).plusDays(1);

        List<Member> candidates = memberRepository.findAllByFcmTokenIsNotNullAndCoupleIsNotNull();
        for (Member member : candidates) {
            NotificationSetting setting = notificationSettingService.getOrCreate(member);
            if (!setting.isCalendarEnabled()) continue;
            if (setting.getCalendarReminderHour() != currentHour) continue;

            Long coupleId = member.getCouple().getId();
            List<Schedule> schedules = scheduleRepository.findAllByCoupleIdAndDate(coupleId, tomorrow);
            if (schedules.isEmpty()) continue;

            String body = buildBody(schedules);
            fcmService.sendSchedulePush(member.getFcmToken(), "내일 일정", body);
        }
    }

    private String buildBody(List<Schedule> schedules) {
        if (schedules.size() == 1) {
            return schedules.get(0).getTitle();
        }
        String titles = schedules.stream()
                .map(Schedule::getTitle)
                .collect(Collectors.joining(", "));
        return titles + " 외 " + (schedules.size() - 1) + "개";
    }
}
