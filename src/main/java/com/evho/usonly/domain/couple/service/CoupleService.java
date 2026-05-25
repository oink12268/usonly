package com.evho.usonly.domain.couple.service;

import com.evho.usonly.domain.couple.entity.Couple;
import com.evho.usonly.domain.couple.repository.CoupleRepository;
import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.domain.member.repository.MemberRepository;
import com.evho.usonly.global.exception.CustomException;
import com.evho.usonly.global.exception.ErrorCode;
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
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (me.getCouple() != null) {
            throw new CustomException(ErrorCode.ALREADY_COUPLE);
        }

        // 2. 상대방 찾기 (코드로)
        Member partner = memberRepository.findByInvitationCode(partnerCode)
                .orElseThrow(() -> new CustomException(ErrorCode.INVITE_CODE_NOT_FOUND));

        // 3. 방어 로직 (자기 자신, 이미 커플인 상대)
        if (me.getId().equals(partner.getId())) {
            throw new CustomException(ErrorCode.SELF_CODE_NOT_ALLOWED);
        }
        if (partner.getCouple() != null) {
            throw new CustomException(ErrorCode.PARTNER_ALREADY_COUPLE);
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