package com.evho.usonly.global.resolver;

import com.evho.usonly.domain.member.model.Member;
import com.evho.usonly.domain.member.repository.MemberRepository;
import com.evho.usonly.global.annotation.CurrentMember;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class CurrentMemberArgumentResolver implements HandlerMethodArgumentResolver {

    private final MemberRepository memberRepository;

    // firebaseUid → memberId 캐시 (서버 재시작 전까지 유지)
    // providerId 조회(인덱스 스캔) → id 조회(PK)로 교체해 이후 요청 속도 개선
    private final ConcurrentHashMap<String, Long> uidToMemberIdCache = new ConcurrentHashMap<>();

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentMember.class)
                && parameter.getParameterType().equals(Member.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        String firebaseUid = (String) request.getAttribute("firebaseUid");

        if (firebaseUid == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }

        Long memberId = uidToMemberIdCache.computeIfAbsent(firebaseUid,
                uid -> memberRepository.findByProviderId(uid)
                        .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 회원입니다."))
                        .getId());

        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 회원입니다."));
    }
}
