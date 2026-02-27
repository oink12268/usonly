package com.evho.usonly.domain.chat.repository;

import com.evho.usonly.domain.chat.entity.Chat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    List<Chat> findAllByOrderByCreatedAtAsc();
    // 최신 N개 (내림차순) - 초기 로딩용
    List<Chat> findByOrderByIdDesc(Pageable pageable);
    // before id보다 작은 것들 중 최신 N개 - 더보기용
    List<Chat> findByIdLessThanOrderByIdDesc(Long before, Pageable pageable);
}