package com.evho.usonly.domain.note.repository;

import com.evho.usonly.domain.note.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findAllByCoupleIdAndParentIsNullOrderByUpdatedAtDesc(Long coupleId);
    List<Note> findAllByCoupleIdAndParentIdOrderByUpdatedAtDesc(Long coupleId, Long parentId);
}
