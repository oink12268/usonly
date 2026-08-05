package com.evho.usonly.domain.chat.repository;

import com.evho.usonly.domain.chat.entity.Chat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    List<Chat> findByCoupleIdOrderByCreatedAtAsc(Long coupleId);

    // 검색/이미지 목록: 메시지가 암호화 저장이라 DB에서 내용 기준 필터링이 불가능해서
    // 커플 채팅 전체를 최신순으로 가져온 뒤 앱에서 복호화하며 필터링+페이징한다.
    List<Chat> findByCoupleIdOrderByCreatedAtDesc(Long coupleId);

    List<Chat> findByCoupleIdOrderByIdDesc(Long coupleId, Pageable pageable);

    List<Chat> findByCoupleIdAndIdLessThanOrderByIdDesc(Long coupleId, Long before, Pageable pageable);

    List<Chat> findByCoupleIdAndIdGreaterThanOrderByIdAsc(Long coupleId, Long after, Pageable pageable);

    @Query(value = "SELECT DATE(created_at) as date, COUNT(*) as cnt FROM chat WHERE couple_id = :coupleId GROUP BY DATE(created_at)", nativeQuery = true)
    List<Object[]> findChatCountByDateAndCoupleId(@Param("coupleId") Long coupleId);

    @Query(value = "SELECT * FROM chat WHERE couple_id = :coupleId AND DATE(created_at) = :date ORDER BY created_at ASC", nativeQuery = true)
    List<Chat> findByCoupleIdAndDate(@Param("coupleId") Long coupleId, @Param("date") String date);

    @Query(value = "SELECT DISTINCT DATE(created_at) FROM chat WHERE couple_id = :coupleId AND created_at >= :from ORDER BY DATE(created_at) ASC", nativeQuery = true)
    List<String> findDistinctDatesSinceByCoupleId(@Param("coupleId") Long coupleId, @Param("from") String from);

    void deleteAllByCoupleId(Long coupleId);

    // ===== 백필용 =====

    long countByCoupleIsNull();

    @Query("SELECT DISTINCT c.writerUid FROM Chat c WHERE c.couple IS NULL")
    List<String> findDistinctWriterUidByCoupleIsNull();

    List<Chat> findByCoupleIsNullAndWriterUid(String writerUid);
}