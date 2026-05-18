package com.evho.usonly.domain.notification.dto;

import com.evho.usonly.domain.notification.entity.NotificationSetting;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationSettingResponse {
    private boolean calendarEnabled;
    private boolean chatEnabled;
    private boolean anniversaryEnabled;
    private boolean couponEnabled;
    private int calendarReminderHour;

    public static NotificationSettingResponse of(NotificationSetting s) {
        return new NotificationSettingResponse(
                s.isCalendarEnabled(),
                s.isChatEnabled(),
                s.isAnniversaryEnabled(),
                s.isCouponEnabled(),
                s.getCalendarReminderHour()
        );
    }
}
