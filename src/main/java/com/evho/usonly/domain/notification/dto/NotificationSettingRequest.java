package com.evho.usonly.domain.notification.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NotificationSettingRequest {
    private boolean calendarEnabled;
    private boolean chatEnabled;
    private boolean anniversaryEnabled;
    private boolean couponEnabled;
    private int calendarReminderHour;
}
