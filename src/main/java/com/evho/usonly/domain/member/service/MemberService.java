package com.evho.usonly.domain.member.service;

import com.evho.usonly.domain.member.dto.LoginRequest;
import com.evho.usonly.domain.member.dto.LoginResponse;
import com.evho.usonly.domain.member.dto.MemberCacheDto;
import com.evho.usonly.domain.member.dto.MemberInfoResponse;
import com.evho.usonly.domain.member.dto.MemberPublicResponse;
import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.domain.member.repository.MemberRepository;
import com.evho.usonly.global.exception.CustomException;
import com.evho.usonly.global.exception.ErrorCode;
import com.evho.usonly.global.storage.FileStorageService;
import com.evho.usonly.global.utils.CoupleCodeGenerator;
import lombok.RequiredArgsConstructor;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final FileStorageService fileStorageService;
    private final CacheManager cacheManager;

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
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        return MemberInfoResponse.from(member);
    }

    // providerId로 멤버 정보 조회 (채팅 프로필 이미지/닉네임 표시용). 이메일 등 PII 제외.
    @Transactional(readOnly = true)
    public MemberPublicResponse getMemberInfoByProviderId(String providerId) {
        Member member = memberRepository.findByProviderId(providerId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        return MemberPublicResponse.from(member);
    }

    // 닉네임 업데이트
    @Transactional
    public void updateNickname(Long memberId, String nickname) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        member.updateNickname(nickname);
        evictMemberCachesAfterCommit();
    }

    // 프로필 이미지 업로드 & 업데이트
    @Transactional
    public String updateProfileImage(Long memberId, MultipartFile file) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        try {
            String imageUrl = fileStorageService.storeProfileImage(file);
            member.updateProfileImageUrl(imageUrl);
            evictMemberCachesAfterCommit();
            return imageUrl;
        } catch (IOException e) {
            throw new RuntimeException("프로필 이미지 업로드 중 오류가 발생했습니다.", e);
        }
    }

    @Transactional
    public void updateFcmToken(Long userId, String token) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        member.updateFcmToken(token);
        evictMemberCachesAfterCommit();
    }

    // @CacheEvict를 @Transactional과 같이 걸면 트랜잭션 커밋 전에 캐시부터 지워져서,
    // 그 틈에 다른 요청이 findByProviderId를 호출하면 커밋 전 값(예: 이전 FCM 토큰)이 다시 캐싱되는
    // 경합이 발생한다 (앱 재설치 직후 로그인 시 여러 API가 동시에 몰려 FCM 토큰이 즉시 stale 캐시로
    // 덮여써지고, 그 토큰으로 푸시를 보내면 NotRegistered로 실패하는 문제로 실제 발생함).
    // 커밋이 끝난 뒤에만 캐시를 지우도록 트랜잭션 동기화 콜백을 사용한다.
    private void evictMemberCachesAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            doEvictMemberCaches();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                doEvictMemberCaches();
            }
        });
    }

    private void doEvictMemberCaches() {
        cacheManager.getCache("member:providerId").clear();
        cacheManager.getCache("member:coupleId").clear();
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