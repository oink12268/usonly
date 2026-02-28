package com.evho.usonly.domain.archive.repository;

import com.evho.usonly.domain.archive.entity.Media;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MediaRepository extends JpaRepository<Media, Long> {

    // 커플의 모든 사진(앨범 포함 + 일반)을 촬영일(없으면 업로드일) 기준 최신순으로 페이징 조회
    @Query("SELECT m FROM Media m WHERE m.couple.id = :coupleId " +
           "ORDER BY COALESCE(m.takenAt, m.createdAt) DESC")
    Page<Media> findAllByCoupleIdOrderByDate(@Param("coupleId") Long coupleId, Pageable pageable);
}
