package com.evho.usonly.domain.chat.repository;

import com.evho.usonly.domain.chat.entity.Chat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    List<Chat> findAllByOrderByCreatedAtAsc();
    // 최신 N개 (내림차순) - 초기 로딩용
    List<Chat> findByOrderByIdDesc(Pageable pageable);
    // before id보다 작은 것들 중 최신 N개 - 더보기용
    List<Chat> findByIdLessThanOrderByIdDesc(Long before, Pageable pageable);
    // after id보다 큰 것들 중 오래된 N개 (오름차순) - 답장 이동 후 최신 방향 로드용
    List<Chat> findByIdGreaterThanOrderByIdAsc(Long after, Pageable pageable);

    // 키워드 검색 (이미지 메시지 제외, 최신순)
    @Query("SELECT c FROM Chat c WHERE LOWER(c.message) LIKE LOWER(CONCAT('%', :keyword, '%')) AND c.message NOT LIKE 'IMAGE:%' ORDER BY c.createdAt DESC")
    List<Chat> searchByKeyword(@Param("keyword") String keyword);

    // 날짜별 채팅 갯수 (네이티브 쿼리)
    @Query(value = "SELECT DATE(created_at) as date, COUNT(*) as cnt FROM chat GROUP BY DATE(created_at)", nativeQuery = true)
    List<Object[]> findChatCountByDate();

    // 특정 날짜의 전체 채팅 (오래된 순)
    @Query(value = "SELECT * FROM chat WHERE DATE(created_at) = :date ORDER BY created_at ASC", nativeQuery = true)
    List<Chat> findByDate(@Param("date") String date);

    // 이미지 메시지만 조회 (최신순)
    @Query("SELECT c FROM Chat c WHERE c.message LIKE 'IMAGE:%' ORDER BY c.id DESC")
    List<Chat> findImageMessages();
}