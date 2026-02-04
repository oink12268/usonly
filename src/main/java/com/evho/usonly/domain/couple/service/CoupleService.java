package com.evho.usonly.domain.couple.service;

import com.evho.usonly.domain.couple.dto.CoupleConnectRequest;
import com.evho.usonly.domain.couple.model.Couple;
import com.evho.usonly.domain.member.model.User;
import com.evho.usonly.domain.member.repository.MemberRepository;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class CoupleService {

    private final MemberRepository memberRepository;

    public String connect(CoupleConnectRequest request) throws ExecutionException, InterruptedException {
        // 1. 내 정보 가져오기
        User me = memberRepository.findByUid(request.getMyUid());
        if (me == null) return "내 정보를 찾을 수 없습니다.";
        if (!"WAITING".equals(me.getStatus())) return "이미 커플이거나 매칭 불가 상태입니다.";

        // 2. 상대방 찾기 (코드로)
        User partner = memberRepository.findByInviteCode(request.getPartnerCode());
        if (partner == null) return "잘못된 초대 코드입니다.";
        if (partner.getUid().equals(me.getUid())) return "본인 코드는 입력할 수 없습니다.";
        if (!"WAITING".equals(partner.getStatus())) return "상대방은 이미 커플입니다.";

        // 3. 커플 방 생성 (ID는 랜덤 생성)
        String coupleId = UUID.randomUUID().toString();
        Couple couple = Couple.builder()
                .coupleId(coupleId)
                .members(Arrays.asList(me.getUid(), partner.getUid()))
                .startDate(LocalDateTime.now().toString()) // 오늘부터 1일
                .createdAt(LocalDateTime.now().toString())
                .build();

        // 4. DB 업데이트 (원래는 Transaction으로 묶어야 안전하지만, 일단 순차 실행)
        Firestore db = FirestoreClient.getFirestore();

        // (1) 커플방 저장
        db.collection("couples").document(coupleId).set(couple);

        // (2) 내 상태 변경 (SOLO -> COUPLE)
        me.setPartnerUid(partner.getUid());
        me.setCoupleId(coupleId);
        me.setStatus("COUPLE");
        memberRepository.save(me);

        // (3) 상대방 상태 변경
        partner.setPartnerUid(me.getUid());
        partner.setCoupleId(coupleId);
        partner.setStatus("COUPLE");
        memberRepository.save(partner);

        return "매칭 성공! 커플이 되었습니다. ID: " + coupleId;
    }
}