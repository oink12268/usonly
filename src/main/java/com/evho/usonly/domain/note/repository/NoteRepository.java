package com.evho.usonly.domain.note.repository;

import com.evho.usonly.domain.note.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {

    @Query("SELECT n FROM Note n WHERE n.couple.id = :coupleId AND n.parent IS NULL " +
           "AND (n.isPrivate = false OR n.createdBy.id = :memberId) " +
           "ORDER BY n.sortOrder ASC, n.id DESC")
    List<Note> findVisibleRootNotes(@Param("coupleId") Long coupleId, @Param("memberId") Long memberId);

    @Query("SELECT n FROM Note n WHERE n.couple.id = :coupleId AND n.parent.id = :parentId " +
           "AND (n.isPrivate = false OR n.createdBy.id = :memberId) " +
           "ORDER BY n.sortOrder ASC, n.id DESC")
    List<Note> findVisibleChildNotes(@Param("coupleId") Long coupleId, @Param("parentId") Long parentId,
                                     @Param("memberId") Long memberId);

    @Query("SELECT MIN(n.sortOrder) FROM Note n WHERE n.couple.id = :coupleId " +
           "AND (:parentId IS NULL AND n.parent IS NULL OR n.parent.id = :parentId)")
    Long findMinSortOrder(@Param("coupleId") Long coupleId, @Param("parentId") Long parentId);
}
