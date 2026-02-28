package com.evho.usonly.domain.member.service;

import com.evho.usonly.domain.member.dto.LoginRequest;
import com.evho.usonly.domain.member.dto.LoginResponse;
import com.evho.usonly.domain.member.dto.MemberCacheDto;
import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.domain.member.repository.MemberRepository;
import com.evho.usonly.global.utils.CoupleCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional
    public LoginResponse loginOrSignup(LoginRequest request) {
        // 1. DB 조회: 이미 있는 회원인가? (providerId로 식별 추천)
        Member member = memberRepository.findByProviderAndProviderId(request.getProvider(), request.getProviderId())
                .orElseGet(() -> {
                    // 2. 신규 회원: 초대코드 발급 및 저장
                    String newCode = createUniqueInvitationCode();

                    Member newMember = Member.builder()
                            .email(request.getEmail())
                            .nickname(request.getNickname())
                            .provider(request.getProvider())
                            .providerId(request.getProviderId())
                            .profileImageUrl(request.getProfileImageUrl())
                            .invitationCode(newCode) // ★ 코드 발급!
                            .role(Member.Role.USER)
                            .build();

                    return memberRepository.save(newMember);
                });

        // 3. 기존 회원: 정보 업데이트 (Dirty Checking)
        if (request.getNickname() != null && !request.getNickname().equals(member.getNickname())) {
            member.setNickname(request.getNickname());
        }

        // 4. 응답 객체 생성 (초대코드 실어서 보냄)
        return LoginResponse.builder()
                .memberId(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .invitationCode(member.getInvitationCode()) // ★ 프론트로 전송
                .coupleId(member.getCouple() != null ? member.getCouple().getId() : null)
                .build();
    }

    @Transactional(readOnly = true)
    public String getNicknameByProviderId(String providerId) {
        return memberRepository.findByProviderId(providerId)
                .map(Member::getNickname)
                .orElse("알 수 없음");
    }

    @Caching(evict = {
            @CacheEvict(value = "member:providerId", allEntries = true),
            @CacheEvict(value = "member:coupleId", allEntries = true)
    })
    @Transactional
    public void updateFcmToken(Long userId, String token) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다."));
        member.updateFcmToken(token);
    }

    @Cacheable(value = "member:providerId", key = "#providerId")
    @Transactional(readOnly = true)
    public MemberCacheDto findByProviderId(String providerId) {
        return memberRepository.findByProviderId(providerId)
                .map(MemberCacheDto::from)
                .orElse(null);
    }

    @Cacheable(value = "member:coupleId", key = "#coupleId")
    @Transactional(readOnly = true)
    public List<MemberCacheDto> findAllByCoupleId(Long coupleId) {
        return memberRepository.findAllByCoupleId(coupleId).stream()
                .map(MemberCacheDto::from)
                .toList();
    }

    // 중복 없는 코드 생성기
    private String createUniqueInvitationCode() {
        String code;
        do {
            code = CoupleCodeGenerator.generateCode(); // 아까 만든 유틸 클래스
        } while (memberRepository.existsByInvitationCode(code));
        return code;
    }
}