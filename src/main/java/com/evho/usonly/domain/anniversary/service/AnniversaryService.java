package com.evho.usonly.domain.anniversary.service;

import com.evho.usonly.domain.anniversary.dto.AnniversaryRequest;
import com.evho.usonly.domain.anniversary.dto.AnniversaryResponse;
import com.evho.usonly.domain.anniversary.model.Anniversary;
import com.evho.usonly.domain.anniversary.repository.AnniversaryRepository;
import com.evho.usonly.domain.couple.model.Couple;
import com.evho.usonly.domain.member.model.Member;
import com.evho.usonly.domain.member.repository.MemberRepository;
import com.evho.usonly.global.utils.LunarConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnniversaryService {

    private final AnniversaryRepository anniversaryRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Long create(Long userId, AnniversaryRequest request) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다."));
        Couple couple = member.getCouple();

        LocalDate date;
        if (request.isLunar()) {
            // 음력 → 올해 기준 양력 변환
            int year = LocalDate.now().getYear();
            LocalDate converted = LunarConverter.toSolar(year, request.getLunarMonth(), request.getLunarDay());
            date = converted != null ? converted : LocalDate.now();
        } else {
            date = LocalDate.parse(request.getDate());
        }

        Anniversary anniversary = Anniversary.builder()
                .title(request.getTitle())
                .date(date)
                .recurring(request.isRecurring())
                .lunar(request.isLunar())
                .lunarMonth(request.getLunarMonth())
                .lunarDay(request.getLunarDay())
                .couple(couple)
                .build();

        return anniversaryRepository.save(anniversary).getId();
    }

    @Transactional(readOnly = true)
    public List<AnniversaryResponse> getAll(Long userId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다."));

        LocalDate today = LocalDate.now();
        return anniversaryRepository.findAllByCoupleIdOrderByDateAsc(member.getCouple().getId())
                .stream()
                .map(ann -> AnniversaryResponse.of(ann, getNextOccurrence(ann, today)))
                .collect(Collectors.toList());
    }

    @Transactional
    public void update(Long anniversaryId, AnniversaryRequest request, Long memberId) {
        Anniversary anniversary = anniversaryRepository.findById(anniversaryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 기념일이 없습니다."));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다."));
        if (!anniversary.getCouple().getId().equals(member.getCouple().getId())) {
            throw new IllegalStateException("권한이 없습니다.");
        }

        LocalDate date;
        if (request.isLunar()) {
            int year = LocalDate.now().getYear();
            LocalDate converted = LunarConverter.toSolar(year, request.getLunarMonth(), request.getLunarDay());
            date = converted != null ? converted : anniversary.getDate();
        } else {
            date = LocalDate.parse(request.getDate());
        }

        anniversary.update(request.getTitle(), date, request.isRecurring(),
                request.isLunar(), request.getLunarMonth(), request.getLunarDay());
    }

    @Transactional
    public void delete(Long anniversaryId, Long memberId) {
        Anniversary anniversary = anniversaryRepository.findById(anniversaryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 기념일이 없습니다."));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다."));
        if (!anniversary.getCouple().getId().equals(member.getCouple().getId())) {
            throw new IllegalStateException("권한이 없습니다.");
        }
        anniversaryRepository.deleteById(anniversaryId);
    }

    /**
     * 기념일의 다음 발생 날짜 계산
     * - 음력+반복: 올해 음력 → 양력 변환, 지났으면 내년
     * - 음력+단일: 저장된 date 그대로 (생성 시 이미 변환됨)
     * - 양력+반복: 올해 날짜, 지났으면 내년
     * - 양력+단일: 저장된 date 그대로
     */
    public LocalDate getNextOccurrence(Anniversary ann, LocalDate today) {
        if (ann.isLunar() && ann.isRecurring()) {
            // 올해 음력 → 양력 변환
            LocalDate thisYear = LunarConverter.toSolar(today.getYear(), ann.getLunarMonth(), ann.getLunarDay());
            if (thisYear == null) return ann.getDate();
            if (!thisYear.isBefore(today)) return thisYear;
            // 지났으면 내년
            LocalDate nextYear = LunarConverter.toSolar(today.getYear() + 1, ann.getLunarMonth(), ann.getLunarDay());
            return nextYear != null ? nextYear : thisYear.plusYears(1);
        }

        if (!ann.isLunar() && ann.isRecurring()) {
            LocalDate thisYear = ann.getDate().withYear(today.getYear());
            if (!thisYear.isBefore(today)) return thisYear;
            return thisYear.plusYears(1);
        }

        // 단일 기념일 (음력/양력 모두): 저장된 date 그대로
        return ann.getDate();
    }
}
