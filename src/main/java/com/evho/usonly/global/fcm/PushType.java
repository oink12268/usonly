package com.evho.usonly.global.fcm;

public enum PushType {
    CHAT, SCHEDULE, COUPON, ANNIVERSARY, CLEAR_CHAT;

    public String value() {
        return name().toLowerCase();
    }
}
