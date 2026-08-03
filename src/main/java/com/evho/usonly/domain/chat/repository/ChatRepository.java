package com.evho.usonly.domain.chat.repository;

import com.evho.usonly.domain.chat.entity.Chat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    // ===== 아래 2개는 아직 커플 격리가 안 된 DailyEmbeddingService(AI 임베딩 파이프라인) 전용.
    // 컨트롤러에서 쓰던 나머지 전역 조회 메서드는 coupleId 버전으로 교체 완료되어 삭제함. =====

    // 특정 날짜의 전체 채팅 (오래된 순)
    @Query(value = "SELECT * FROM chat WHERE DATE(created_at) = :date ORDER BY created_at ASC", nativeQuery = true)
    List<Chat> findByDate(@Param("date") String date);

    // 특정 기간 내 채팅이 있는 날짜 목록
    @Query(value = "SELECT DISTINCT DATE(created_at) FROM chat WHERE created_at >= :from ORDER BY DATE(created_at) ASC", nativeQuery = true)
    List<String> findDistinctDatesSince(@Param("from") String from);

    // ===== coupleId 격리 버전 =====

    List<Chat> findByCoupleIdOrderByCreatedAtAsc(Long coupleId);

    List<Chat> findByCoupleIdOrderByIdDesc(Long coupleId, Pageable pageable);

    List<Chat> findByCoupleIdAndIdLessThanOrderByIdDesc(Long coupleId, Long before, Pageable pageable);

    List<Chat> findByCoupleIdAndIdGreaterThanOrderByIdAsc(Long coupleId, Long after, Pageable pageable);

    // keyword는 호출 전에 ChatController#escapeLike로 %, _, \ 이스케이프 처리되어 들어와야 함
    @Query("SELECT c FROM Chat c WHERE c.couple.id = :coupleId AND LOWER(c.message) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\' AND c.message NOT LIKE 'IMAGE:%' ORDER BY c.createdAt DESC")
    List<Chat> searchByCoupleIdAndKeyword(@Param("coupleId") Long coupleId, @Param("keyword") String keyword);

    @Query(value = "SELECT DATE(created_at) as date, COUNT(*) as cnt FROM chat WHERE couple_id = :coupleId GROUP BY DATE(created_at)", nativeQuery = true)
    List<Object[]> findChatCountByDateAndCoupleId(@Param("coupleId") Long coupleId);

    @Query(value = "SELECT * FROM chat WHERE couple_id = :coupleId AND DATE(created_at) = :date ORDER BY created_at ASC", nativeQuery = true)
    List<Chat> findByCoupleIdAndDate(@Param("coupleId") Long coupleId, @Param("date") String date);

    @Query("SELECT c FROM Chat c WHERE c.couple.id = :coupleId AND c.message LIKE 'IMAGE:%' ORDER BY c.id DESC")
    List<Chat> findImageMessagesByCoupleId(@Param("coupleId") Long coupleId, Pageable pageable);

    @Query(value = "SELECT DISTINCT DATE(created_at) FROM chat WHERE couple_id = :coupleId AND created_at >= :from ORDER BY DATE(created_at) ASC", nativeQuery = true)
    List<String> findDistinctDatesSinceByCoupleId(@Param("coupleId") Long coupleId, @Param("from") String from);

    void deleteAllByCoupleId(Long coupleId);

    // ===== 백필용 =====

    long countByCoupleIsNull();

    @Query("SELECT DISTINCT c.writerUid FROM Chat c WHERE c.couple IS NULL")
    List<String> findDistinctWriterUidByCoupleIsNull();

    List<Chat> findByCoupleIsNullAndWriterUid(String writerUid);
}