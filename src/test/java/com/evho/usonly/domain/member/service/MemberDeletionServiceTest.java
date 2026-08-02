package com.evho.usonly.domain.member.service;

import com.evho.usonly.domain.anniversary.entity.Anniversary;
import com.evho.usonly.domain.anniversary.repository.AnniversaryRepository;
import com.evho.usonly.domain.archive.entity.Album;
import com.evho.usonly.domain.archive.entity.Media;
import com.evho.usonly.domain.archive.repository.AlbumRepository;
import com.evho.usonly.domain.archive.repository.MediaRepository;
import com.evho.usonly.domain.chat.entity.Chat;
import com.evho.usonly.domain.chat.repository.ChatRepository;
import com.evho.usonly.domain.couple.entity.Couple;
import com.evho.usonly.domain.couple.repository.CoupleRepository;
import com.evho.usonly.domain.coupon.entity.Coupon;
import com.evho.usonly.domain.coupon.repository.CouponRepository;
import com.evho.usonly.domain.game.entity.GameScore;
import com.evho.usonly.domain.game.repository.GameScoreRepository;
import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.domain.member.repository.MemberRepository;
import com.evho.usonly.domain.note.entity.Note;
import com.evho.usonly.domain.note.repository.NoteRepository;
import com.evho.usonly.domain.notification.entity.NotificationSetting;
import com.evho.usonly.domain.notification.repository.NotificationSettingRepository;
import com.evho.usonly.domain.schedule.entity.Schedule;
import com.evho.usonly.domain.schedule.repository.ScheduleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 운영 DB에 붙어서 돌리되, @Transactional로 테스트 종료 시 자동 롤백되므로
// 운영 데이터에는 아무 흔적도 남지 않음. 가짜 커플/멤버를 만들어 탈퇴 cascade를 검증한다.
@SpringBootTest
@Transactional
class MemberDeletionServiceTest {

    @Autowired private MemberDeletionService memberDeletionService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private CoupleRepository coupleRepository;
    @Autowired private ChatRepository chatRepository;
    @Autowired private NoteRepository noteRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private MediaRepository mediaRepository;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private AnniversaryRepository anniversaryRepository;
    @Autowired private CouponRepository couponRepository;
    @Autowired private GameScoreRepository gameScoreRepository;
    @Autowired private NotificationSettingRepository notificationSettingRepository;

    @Test
    void 탈퇴하면_커플_공유데이터는_지워지고_파트너_계정은_연결만_해제된다() {
        // given: 가짜 커플 + 멤버 2명 + 도메인별 데이터 하나씩
        Couple couple = coupleRepository.save(Couple.builder()
                .startDate(LocalDate.now())
                .invitationCode("TEST-DELETE-CODE")
                .build());

        Member me = memberRepository.save(Member.builder()
                .email("test-delete-me@example.com")
                .nickname("테스트나")
                .provider("GOOGLE")
                .providerId("test-uid-me-should-not-exist")
                .role(Member.Role.USER)
                .couple(couple)
                .build());

        Member partner = memberRepository.save(Member.builder()
                .email("test-delete-partner@example.com")
                .nickname("테스트파트너")
                .provider("GOOGLE")
                .providerId("test-uid-partner-should-not-exist")
                .role(Member.Role.USER)
                .couple(couple)
                .build());

        chatRepository.save(Chat.builder()
                .message("테스트 채팅").writerUid(me.getProviderId()).sendTime("오후 1:00")
                .couple(couple).build());

        noteRepository.save(Note.builder()
                .couple(couple).title("테스트 메모").content("내용").createdBy(me).lastEditedBy(me)
                .isPrivate(false).sortOrder(0L).build());

        Album album = albumRepository.save(Album.builder()
                .title("테스트 앨범").couple(couple).sortOrder(0).build());
        mediaRepository.save(Media.builder()
                .couple(couple).album(album)
                .mediaUrl("https://example.com/does-not-exist.jpg")
                .mediaType("IMAGE").build());

        scheduleRepository.save(Schedule.builder()
                .title("테스트 일정").memo("메모").date(LocalDate.now()).couple(couple).writer(me).build());

        anniversaryRepository.save(Anniversary.builder()
                .title("테스트 기념일").date(LocalDate.now()).recurring(false).lunar(false).couple(couple).build());

        couponRepository.save(Coupon.builder()
                .couple(couple).imageUrl("https://example.com/coupon.jpg").storeName("테스트가게").build());

        gameScoreRepository.save(GameScore.builder()
                .member(me).gameType("flappy").score(100).playedAt(LocalDateTime.now()).build());
        gameScoreRepository.save(GameScore.builder()
                .member(partner).gameType("flappy").score(50).playedAt(LocalDateTime.now()).build());

        notificationSettingRepository.save(NotificationSetting.builder().member(me).build());
        notificationSettingRepository.save(NotificationSetting.builder().member(partner).build());

        Long coupleId = couple.getId();
        Long meId = me.getId();
        Long partnerId = partner.getId();

        // when: me가 탈퇴
        memberDeletionService.deleteAccount(me);

        // then
        assertThat(memberRepository.findById(meId)).isEmpty(); // 탈퇴 계정 삭제됨
        assertThat(memberRepository.findById(partnerId)).isPresent(); // 파트너 계정은 유지
        assertThat(memberRepository.findById(partnerId).get().getCouple()).isNull(); // 커플 연결만 해제

        assertThat(coupleRepository.findById(coupleId)).isEmpty();
        assertThat(chatRepository.findByCoupleIdOrderByCreatedAtAsc(coupleId)).isEmpty();
        assertThat(albumRepository.findAllByCoupleId(coupleId)).isEmpty();
        assertThat(mediaRepository.findAllByCoupleId(coupleId)).isEmpty();
        assertThat(anniversaryRepository.findAllByCoupleIdOrderByDateAsc(coupleId)).isEmpty();

        // 파트너 개인 데이터(게임 기록/알림 설정)는 삭제 대상이 아니므로 유지
        assertThat(notificationSettingRepository.findByMemberId(partnerId)).isPresent();
    }
}
