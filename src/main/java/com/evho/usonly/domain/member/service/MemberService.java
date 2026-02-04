package com.evho.usonly.domain.member.service;

import com.evho.usonly.domain.member.dto.MemberJoinRequest; // DTO import
import com.evho.usonly.domain.member.model.User;
import com.evho.usonly.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    // 파라미터가 (String, String...) 에서 (MemberJoinRequest) 로 깔끔하게 변경됨!
    public User join(MemberJoinRequest request) throws ExecutionException, InterruptedException {

        // 1. 랜덤 코드 생성
        String randomCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        // 2. User 객체 생성 (DTO에서 값 꺼내기)
        User user = User.builder()
                .uid(request.getUid())
                .nickname(request.getNickname())
                .email(request.getEmail()) // User 모델에 email 필드 추가하셨죠?
                .myCode(randomCode)
                .status("WAITING")
                .build();

        // 3. 저장
        return memberRepository.save(user);
    }

    public User getMember(String uid) throws ExecutionException, InterruptedException {
        // 리포지토리야, 이 UID 가진 사람 찾아와!
        return memberRepository.findByUid(uid);
    }
}