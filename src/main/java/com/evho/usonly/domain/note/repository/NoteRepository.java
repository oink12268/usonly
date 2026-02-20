package com.evho.usonly.domain.note.repository;

import com.evho.usonly.domain.note.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findAllByCoupleIdOrderByUpdatedAtDesc(Long coupleId);
}
