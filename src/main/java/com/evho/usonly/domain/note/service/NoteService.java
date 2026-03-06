package com.evho.usonly.domain.note.service;

import com.evho.usonly.domain.couple.entity.Couple;
import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.domain.note.dto.NoteRequest;
import com.evho.usonly.domain.note.dto.NoteResponse;
import com.evho.usonly.domain.note.entity.Note;
import com.evho.usonly.domain.note.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;

    @Transactional(readOnly = true)
    public List<NoteResponse> getNotes(Long coupleId, Long parentId) {
        List<Note> notes = (parentId == null)
                ? noteRepository.findAllByCoupleIdAndParentIsNullOrderByUpdatedAtDesc(coupleId)
                : noteRepository.findAllByCoupleIdAndParentIdOrderByUpdatedAtDesc(coupleId, parentId);
        return notes.stream().map(NoteResponse::new).toList();
    }

    @Transactional
    public NoteResponse createNote(NoteRequest dto, Member member) {
        Couple couple = member.getCouple();
        if (couple == null) {
            throw new IllegalStateException("커플 연결 후 사용할 수 있습니다.");
        }

        Note parent = null;
        if (dto.getParentId() != null) {
            parent = noteRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("상위 메모를 찾을 수 없습니다."));
        }

        Note note = Note.builder()
                .couple(couple)
                .title(dto.getTitle())
                .content(dto.getContent())
                .parent(parent)
                .lastEditedBy(member)
                .build();

        noteRepository.save(note);
        return new NoteResponse(note);
    }

    @Transactional
    public NoteResponse updateNote(Long noteId, NoteRequest dto, Member member) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("메모를 찾을 수 없습니다."));

        if (!note.getCouple().getId().equals(member.getCouple().getId())) {
            throw new IllegalStateException("접근 권한이 없습니다.");
        }

        note.setTitle(dto.getTitle());
        note.setContent(dto.getContent());
        note.setLastEditedBy(member);

        return new NoteResponse(note);
    }

    @Transactional
    public void deleteNote(Long noteId, Member member) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("메모를 찾을 수 없습니다."));

        if (!note.getCouple().getId().equals(member.getCouple().getId())) {
            throw new IllegalStateException("접근 권한이 없습니다.");
        }

        noteRepository.delete(note);
    }
}
