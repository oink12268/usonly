package com.evho.usonly.domain.chat.repository;

import com.evho.usonly.domain.chat.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    List<Chat> findAllByOrderByCreatedAtAsc();
}