package com.evho.usonly.domain.anniversary.service;

import com.evho.usonly.domain.anniversary.dto.AnniversaryRequest;
import com.evho.usonly.domain.anniversary.dto.AnniversaryResponse;
import com.evho.usonly.domain.anniversary.model.Anniversary;
import com.evho.usonly.domain.anniversary.repository.AnniversaryRepository;
import com.evho.usonly.domain.couple.model.Couple;
import com.evho.usonly.domain.member.model.Member;
import com.evho.usonly.domain.member.repository.MemberRepository;
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

        Anniversary anniversary = Anniversary.builder()
                .title(request.getTitle())
                .date(LocalDate.parse(request.getDate()))
                .recurring(request.isRecurring())
                .couple(couple)
                .build();

        return anniversaryRepository.save(anniversary).getId();
    }

    @Transactional(readOnly = true)
    public List<AnniversaryResponse> getAll(Long userId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다."));

        return anniversaryRepository.findAllByCoupleIdOrderByDateAsc(member.getCouple().getId())
                .stream()
                .map(AnniversaryResponse::of)
                .collect(Collectors.toList());
    }

    @Transactional
    public void update(Long anniversaryId, AnniversaryRequest request) {
        Anniversary anniversary = anniversaryRepository.findById(anniversaryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 기념일이 없습니다."));

        anniversary.update(request.getTitle(), LocalDate.parse(request.getDate()), request.isRecurring());
    }

    @Transactional
    public void delete(Long anniversaryId) {
        anniversaryRepository.deleteById(anniversaryId);
    }
}
