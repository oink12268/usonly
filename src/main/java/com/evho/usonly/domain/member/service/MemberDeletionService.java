package com.evho.usonly.domain.member.service;

import com.evho.usonly.domain.anniversary.repository.AnniversaryRepository;
import com.evho.usonly.domain.archive.entity.Media;
import com.evho.usonly.domain.archive.repository.AlbumRepository;
import com.evho.usonly.domain.archive.repository.MediaRepository;
import com.evho.usonly.domain.chat.entity.Chat;
import com.evho.usonly.domain.chat.repository.ChatRepository;
import com.evho.usonly.domain.couple.entity.Couple;
import com.evho.usonly.domain.couple.repository.CoupleRepository;
import com.evho.usonly.domain.coupon.repository.CouponRepository;
import com.evho.usonly.domain.game.repository.GameScoreRepository;
import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.domain.member.repository.MemberRepository;
import com.evho.usonly.domain.note.repository.NoteRepository;
import com.evho.usonly.domain.notification.repository.NotificationSettingRepository;
import com.evho.usonly.domain.schedule.repository.ScheduleRepository;
import com.evho.usonly.global.storage.FileStorageService;
import com.google.firebase.auth.FirebaseAuth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 회원탈퇴: 커플이 함께 만든 데이터(채팅/앨범/메모/일정/기념일/쿠폰)는 전부 삭제하고,
// 파트너는 계정/개인데이터는 그대로 둔 채 커플 연결만 해제한다. 탈퇴 당사자만 계정 자체(DB + Firebase Auth)를 지운다.
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberDeletionService {

    private final MemberRepository memberRepository;
    private final CoupleRepository coupleRepository;
    private final MediaRepository mediaRepository;
    private final AlbumRepository albumRepository;
    private final ChatRepository chatRepository;
    private final NoteRepository noteRepository;
    private final ScheduleRepository scheduleRepository;
    private final AnniversaryRepository anniversaryRepository;
    private final CouponRepository couponRepository;
    private final GameScoreRepository gameScoreRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public void deleteAccount(Member me) {
        Couple couple = me.getCouple();

        if (couple != null) {
            Long coupleId = couple.getId();
            deleteCoupleSharedData(coupleId);

            memberRepository.findAllByCoupleId(coupleId).stream()
                    .filter(m -> !m.getId().equals(me.getId()))
                    .findFirst()
                    .ifPresent(partner -> partner.setCouple(null));

            // me도 연결 해제: 안 하면 managed 상태인 me가 곧 삭제될 couple을 계속 참조하고 있어서,
            // 이후 벌크 삭제 쿼리들이 auto-flush될 때 Hibernate가 TransientObjectException을 던짐
            me.setCouple(null);

            coupleRepository.deleteById(coupleId);
        }

        gameScoreRepository.deleteAllByMemberId(me.getId());
        notificationSettingRepository.findByMemberId(me.getId())
                .ifPresent(notificationSettingRepository::delete);
        if (me.getProfileImageUrl() != null) {
            fileStorageService.delete(me.getProfileImageUrl());
        }

        memberRepository.delete(me);

        try {
            FirebaseAuth.getInstance().deleteUser(me.getProviderId());
        } catch (Exception e) {
            log.warn("Firebase Auth 계정 삭제 실패 (DB 삭제는 이미 완료됨): providerId={}", me.getProviderId(), e);
        }
    }

    // 실물 파일 삭제 후, 커플이 공유하던 DB 데이터 전부 삭제 (Media → Album 순서로: Media가 Album을 FK로 참조)
    private void deleteCoupleSharedData(Long coupleId) {
        List<Media> mediaList = mediaRepository.findAllByCoupleId(coupleId);
        for (Media media : mediaList) {
            fileStorageService.delete(media.getMediaUrl());
            fileStorageService.delete(media.getThumbnailUrl());
        }

        List<Chat> chats = chatRepository.findByCoupleIdOrderByCreatedAtAsc(coupleId);
        for (Chat chat : chats) {
            String message = chat.getMessage();
            if (message != null && (message.startsWith("IMAGE:") || message.startsWith("FILE:"))) {
                fileStorageService.delete(message.substring(message.indexOf(':') + 1));
            }
        }

        mediaRepository.deleteAllByCoupleId(coupleId);
        albumRepository.deleteAllByCoupleId(coupleId);
        chatRepository.deleteAllByCoupleId(coupleId);
        noteRepository.deleteAllByCoupleId(coupleId);
        scheduleRepository.deleteAllByCoupleId(coupleId);
        anniversaryRepository.deleteAllByCoupleId(coupleId);
        couponRepository.deleteAllByCoupleId(coupleId);
    }
}
