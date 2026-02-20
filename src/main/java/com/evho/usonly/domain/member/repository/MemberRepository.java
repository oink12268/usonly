package com.evho.usonly.domain.member.repository;

import com.evho.usonly.domain.member.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    // "이메일로 회원 찾기" (로그인할 때 씀)
    Optional<Member> findByEmail(String email);

    // "소셜ID로 회원 찾기" (더 정확함)
    Optional<Member> findByProviderAndProviderId(String provider, String providerId);

    // 커플 매칭용 (초대 코드로 멤버 찾기)
    Optional<Member> findByInvitationCode(String invitationCode);

    // 생성된 코드가 이미 있는지 확인용
    boolean existsByInvitationCode(String invitationCode);

    // providerId(uid)로 회원 찾기
    Optional<Member> findByProviderId(String providerId);

    // 같은 커플의 멤버 목록
    List<Member> findAllByCoupleId(Long coupleId);
}