package com.evho.usonly.domain.member.service;

import com.evho.usonly.domain.member.dto.LoginRequest;
import com.evho.usonly.domain.member.dto.LoginResponse;
import com.evho.usonly.domain.member.dto.MemberCacheDto;
import com.evho.usonly.domain.member.dto.MemberInfoResponse;
import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.domain.member.repository.MemberRepository;
import com.evho.usonly.global.storage.FileStorageService;
import com.evho.usonly.global.utils.CoupleCodeGenerator;
import lombok.RequiredArgsConstructor;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final FileStorageService fileStorageService;

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

    // 현재 로그인 멤버 정보 조회
    @Transactional(readOnly = true)
    public MemberInfoResponse getMyInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));
        return MemberInfoResponse.from(member);
    }

    // providerId로 멤버 정보 조회 (채팅 프로필 이미지에 사용)
    @Transactional(readOnly = true)
    public MemberInfoResponse getMemberInfoByProviderId(String providerId) {
        Member member = memberRepository.findByProviderId(providerId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));
        return MemberInfoResponse.from(member);
    }

    // 닉네임 업데이트
    @Caching(evict = {
            @CacheEvict(value = "member:providerId", allEntries = true),
            @CacheEvict(value = "member:coupleId", allEntries = true)
    })
    @Transactional
    public void updateNickname(Long memberId, String nickname) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));
        member.updateNickname(nickname);
    }

    // 프로필 이미지 업로드 & 업데이트
    @Caching(evict = {
            @CacheEvict(value = "member:providerId", allEntries = true),
            @CacheEvict(value = "member:coupleId", allEntries = true)
    })
    @Transactional
    public String updateProfileImage(Long memberId, MultipartFile file) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));
        try {
            String imageUrl = fileStorageService.storeImage(file);
            member.updateProfileImageUrl(imageUrl);
            return imageUrl;
        } catch (IOException e) {
            throw new RuntimeException("프로필 이미지 업로드 중 오류가 발생했습니다.", e);
        }
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

    @Caching(evict = {
            @CacheEvict(value = "member:providerId", allEntries = true),
            @CacheEvict(value = "member:coupleId", allEntries = true)
    })
    @Transactional
    public void updateLastReadChatId(String providerId, Long chatId) {
        Member member = memberRepository.findByProviderId(providerId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다."));
        member.updateLastReadChatId(chatId);
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
                .collect(Collectors.toCollection(ArrayList::new));
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