package com.evho.usonly.domain.schedule.repository;

import com.evho.usonly.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findAllByCoupleIdOrderByDateAsc(Long coupleId);

    List<Schedule> findAllByCoupleIdAndDateBetweenOrderByDateAsc(Long coupleId, LocalDate start, LocalDate end);
}
