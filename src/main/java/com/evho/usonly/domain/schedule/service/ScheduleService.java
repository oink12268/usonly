package com.evho.usonly.domain.schedule.service;

import com.evho.usonly.domain.couple.entity.Couple;
import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.domain.schedule.dto.ScheduleRequest;
import com.evho.usonly.domain.schedule.dto.ScheduleResponse;
import com.evho.usonly.domain.schedule.entity.Schedule;
import com.evho.usonly.domain.schedule.repository.ScheduleRepository;
import com.evho.usonly.global.exception.CustomException;
import com.evho.usonly.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    @Transactional
    public Long create(Member member, ScheduleRequest request) {
        Couple couple = member.getCouple();

        Schedule schedule = Schedule.builder()
                .title(request.getTitle())
                .memo(request.getMemo())
                .date(LocalDate.parse(request.getDate()))
                .couple(couple)
                .writer(member)
                .build();

        return scheduleRepository.save(schedule).getId();
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getByMonth(Member member, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        return scheduleRepository.findAllByCoupleIdAndDateBetweenOrderByDateAsc(
                        member.getCouple().getId(), start, end)
                .stream()
                .map(ScheduleResponse::of)
                .collect(Collectors.toList());
    }

    @Transactional
    public void update(Long scheduleId, ScheduleRequest request, Member member) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
        if (!schedule.getCouple().getId().equals(member.getCouple().getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        schedule.update(request.getTitle(), request.getMemo(), LocalDate.parse(request.getDate()));
    }

    @Transactional
    public void delete(Long scheduleId, Member member) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
        if (!schedule.getCouple().getId().equals(member.getCouple().getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        scheduleRepository.deleteById(scheduleId);
    }
}
