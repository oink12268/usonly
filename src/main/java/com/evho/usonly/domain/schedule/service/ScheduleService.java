package com.evho.usonly.domain.schedule.service;

import com.evho.usonly.domain.couple.model.Couple;
import com.evho.usonly.domain.member.model.Member;
import com.evho.usonly.domain.member.repository.MemberRepository;
import com.evho.usonly.domain.schedule.dto.ScheduleRequest;
import com.evho.usonly.domain.schedule.dto.ScheduleResponse;
import com.evho.usonly.domain.schedule.model.Schedule;
import com.evho.usonly.domain.schedule.repository.ScheduleRepository;
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
    private final MemberRepository memberRepository;

    @Transactional
    public Long create(Long userId, ScheduleRequest request) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다."));
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
    public List<ScheduleResponse> getByMonth(Long userId, int year, int month) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다."));

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        return scheduleRepository.findAllByCoupleIdAndDateBetweenOrderByDateAsc(
                        member.getCouple().getId(), start, end)
                .stream()
                .map(ScheduleResponse::of)
                .collect(Collectors.toList());
    }

    @Transactional
    public void update(Long scheduleId, ScheduleRequest request, Long memberId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("해당 일정이 없습니다."));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다."));
        if (!schedule.getCouple().getId().equals(member.getCouple().getId())) {
            throw new IllegalStateException("권한이 없습니다.");
        }
        schedule.update(request.getTitle(), request.getMemo(), LocalDate.parse(request.getDate()));
    }

    @Transactional
    public void delete(Long scheduleId, Long memberId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("해당 일정이 없습니다."));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다."));
        if (!schedule.getCouple().getId().equals(member.getCouple().getId())) {
            throw new IllegalStateException("권한이 없습니다.");
        }
        scheduleRepository.deleteById(scheduleId);
    }
}
