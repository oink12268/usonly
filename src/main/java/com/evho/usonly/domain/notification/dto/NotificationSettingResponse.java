package com.evho.usonly.domain.notification.dto;

import com.evho.usonly.domain.notification.entity.NotificationSetting;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationSettingResponse {
    private boolean calendarEnabled;
    private boolean chatEnabled;
    private boolean anniversaryEnabled;
    private int calendarReminderHour;

    public static NotificationSettingResponse from(NotificationSetting s) {
        return NotificationSettingResponse.builder()
                .calendarEnabled(s.isCalendarEnabled())
                .chatEnabled(s.isChatEnabled())
                .anniversaryEnabled(s.isAnniversaryEnabled())
                .calendarReminderHour(s.getCalendarReminderHour())
                .build();
    }
}
