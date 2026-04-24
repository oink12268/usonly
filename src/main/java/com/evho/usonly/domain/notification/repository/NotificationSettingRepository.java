package com.evho.usonly.domain.notification.repository;

import com.evho.usonly.domain.notification.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    Optional<NotificationSetting> findByMemberId(Long memberId);
}
