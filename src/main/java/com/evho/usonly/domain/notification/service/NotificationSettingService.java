package com.evho.usonly.domain.notification.service;

import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.domain.notification.dto.NotificationSettingRequest;
import com.evho.usonly.domain.notification.dto.NotificationSettingResponse;
import com.evho.usonly.domain.notification.entity.NotificationSetting;
import com.evho.usonly.domain.notification.repository.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationSettingService {

    private final NotificationSettingRepository repository;

    @Transactional(readOnly = true)
    public NotificationSetting getOrCreate(Member member) {
        return repository.findByMemberId(member.getId())
                .orElseGet(() -> repository.save(
                        NotificationSetting.builder().member(member).build()
                ));
    }

    public NotificationSettingResponse update(Member member, NotificationSettingRequest req) {
        NotificationSetting setting = repository.findByMemberId(member.getId())
                .orElseGet(() -> NotificationSetting.builder().member(member).build());

        setting.setCalendarEnabled(req.isCalendarEnabled());
        setting.setChatEnabled(req.isChatEnabled());
        setting.setAnniversaryEnabled(req.isAnniversaryEnabled());
        setting.setCalendarReminderHour(req.getCalendarReminderHour());

        return NotificationSettingResponse.from(repository.save(setting));
    }

    // 저장된 설정이 없으면 기본값(true) 반환
    @Transactional(readOnly = true)
    public boolean isChatEnabled(Long memberId) {
        return repository.findByMemberId(memberId)
                .map(NotificationSetting::isChatEnabled)
                .orElse(true);
    }

    @Transactional(readOnly = true)
    public boolean isAnniversaryEnabled(Long memberId) {
        return repository.findByMemberId(memberId)
                .map(NotificationSetting::isAnniversaryEnabled)
                .orElse(true);
    }
}
