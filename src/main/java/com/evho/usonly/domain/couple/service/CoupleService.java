package com.evho.usonly.domain.couple.service;

import com.evho.usonly.domain.couple.entity.Couple;
import com.evho.usonly.domain.couple.repository.CoupleRepository;
import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CoupleService {

    private final MemberRepository memberRepository;
    private final CoupleRepository coupleRepository;

    @Caching(evict = {
            @CacheEvict(value = "member:providerId", allEntries = true),
            @CacheEvict(value = "member:coupleId", allEntries = true)
    })
    @Transactional
    public Long connectCouple(Long myMemberId, String partnerCode) {
        // 1. 내 정보 찾기
        Member me = memberRepository.findById(myMemberId)
                .orElseThrow(() -> new IllegalArgumentException("내 정보를 찾을 수 없습니다."));

        if (me.getCouple() != null) {
            throw new IllegalStateException("이미 커플입니다.");
        }

        // 2. 상대방 찾기 (코드로)
        Member partner = memberRepository.findByInvitationCode(partnerCode)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 초대 코드입니다."));

        // 3. 방어 로직 (자기 자신, 이미 커플인 상대)
        if (me.getId().equals(partner.getId())) {
            throw new IllegalArgumentException("본인의 코드는 입력할 수 없습니다.");
        }
        if (partner.getCouple() != null) {
            throw new IllegalStateException("상대방은 이미 커플입니다.");
        }

        // 4. 커플 생성 & 저장
        Couple newCouple = Couple.builder()
                .startDate(LocalDate.now())
                .invitationCode(partnerCode)
                .build();
        coupleRepository.save(newCouple);

        // 5. 두 사람 모두에게 커플 ID 등록 (Dirty Checking)
        me.setCouple(newCouple);
        partner.setCouple(newCouple);

        return newCouple.getId();
    }
}