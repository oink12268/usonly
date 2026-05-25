package com.evho.usonly.domain.note.service;

import com.evho.usonly.domain.couple.entity.Couple;
import com.evho.usonly.domain.member.entity.Member;
import com.evho.usonly.domain.note.dto.NoteReorderRequest;
import com.evho.usonly.domain.note.dto.NoteRequest;
import com.evho.usonly.domain.note.dto.NoteResponse;
import com.evho.usonly.domain.note.entity.Note;
import com.evho.usonly.domain.note.repository.NoteRepository;
import com.evho.usonly.global.exception.CustomException;
import com.evho.usonly.global.exception.ErrorCode;
import com.evho.usonly.global.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final FileStorageService fileStorageService;

    // content에서 메모 이미지 URL 추출 (마크다운 + Delta JSON 모두 대응)
    private Set<String> extractImageUrls(String content) {
        Set<String> urls = new HashSet<>();
        if (content == null || content.isBlank()) return urls;
        Matcher m = Pattern.compile(Pattern.quote(fileStorageService.getBaseUrl()) + "notes/[^\"\\s)]+").matcher(content);
        while (m.find()) urls.add(m.group());
        return urls;
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> getNotes(Long coupleId, Long parentId, Long memberId) {
        List<Note> notes = (parentId == null)
                ? noteRepository.findVisibleRootNotes(coupleId, memberId)
                : noteRepository.findVisibleChildNotes(coupleId, parentId, memberId);
        return notes.stream().map(NoteResponse::new).toList();
    }

    @Transactional
    public NoteResponse createNote(NoteRequest dto, Member member) {
        Couple couple = member.getCouple();
        if (couple == null) {
            throw new CustomException(ErrorCode.COUPLE_REQUIRED);
        }

        Note parent = null;
        if (dto.getParentId() != null) {
            parent = noteRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new CustomException(ErrorCode.NOTE_NOT_FOUND));
        }

        Long minSortOrder = noteRepository.findMinSortOrder(couple.getId(),
                parent != null ? parent.getId() : null);
        long newSortOrder = (minSortOrder != null) ? minSortOrder - 1000 : 0;

        Note note = Note.builder()
                .couple(couple)
                .title(dto.getTitle())
                .content(dto.getContent())
                .parent(parent)
                .createdBy(member)
                .lastEditedBy(member)
                .isPrivate(dto.isPrivate())
                .sortOrder(newSortOrder)
                .build();

        noteRepository.save(note);
        return new NoteResponse(note);
    }

    @Transactional
    public NoteResponse updateNote(Long noteId, NoteRequest dto, Member member) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTE_NOT_FOUND));

        if (!note.getCouple().getId().equals(member.getCouple().getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 이전 content에 있었지만 새 content에 없는 이미지 삭제
        Set<String> oldUrls = extractImageUrls(note.getContent());
        Set<String> newUrls = extractImageUrls(dto.getContent());
        oldUrls.stream()
                .filter(url -> !newUrls.contains(url))
                .forEach(fileStorageService::delete);

        note.setTitle(dto.getTitle());
        note.setContent(dto.getContent());
        note.setLastEditedBy(member);
        // private 토글은 본인(createdBy)만 가능
        if (note.getCreatedBy() == null || note.getCreatedBy().getId().equals(member.getId())) {
            note.setPrivate(dto.isPrivate());
        }

        return new NoteResponse(note);
    }

    @Transactional
    public void reorderNotes(NoteReorderRequest dto, Member member) {
        Long coupleId = member.getCouple().getId();
        List<Long> ids = dto.getOrderedIds();

        Map<Long, Note> noteMap = noteRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Note::getId, n -> n));

        for (int i = 0; i < ids.size(); i++) {
            Note note = noteMap.get(ids.get(i));
            if (note != null && note.getCouple().getId().equals(coupleId)) {
                note.setSortOrder((long) i * 1000);
            }
        }
    }

    @Transactional
    public NoteResponse moveNote(Long noteId, Long targetParentId, Member member) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTE_NOT_FOUND));

        if (!note.getCouple().getId().equals(member.getCouple().getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 자기 자신으로 이동 방지
        if (noteId.equals(targetParentId)) {
            throw new CustomException(ErrorCode.NOTE_SELF_MOVE);
        }

        Note newParent = null;
        if (targetParentId != null) {
            newParent = noteRepository.findById(targetParentId)
                    .orElseThrow(() -> new CustomException(ErrorCode.NOTE_NOT_FOUND));

            // 순환참조 방지: targetParent가 note의 자식(또는 자손)인지 확인
            Note cursor = newParent;
            while (cursor.getParent() != null) {
                if (cursor.getParent().getId().equals(noteId)) {
                    throw new CustomException(ErrorCode.NOTE_DESCENDANT_MOVE);
                }
                cursor = cursor.getParent();
            }
        }

        note.setParent(newParent);
        return new NoteResponse(note);
    }

    @Transactional
    public void deleteNote(Long noteId, Member member) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTE_NOT_FOUND));

        if (!note.getCouple().getId().equals(member.getCouple().getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 메모에 포함된 이미지 파일 모두 삭제
        extractImageUrls(note.getContent()).forEach(fileStorageService::delete);

        noteRepository.delete(note);
    }
}
