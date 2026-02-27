package com.evho.usonly.domain.archive.repository;

import com.evho.usonly.domain.archive.model.Album;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlbumRepository extends JpaRepository<Album, Long> {
    List<Album> findAllByCoupleId(Long coupleId);

    // sortOrder가 있으면 순서대로, null이면 최신순(id DESC)으로 표시
    @Query(value = "SELECT a FROM Album a WHERE a.couple.id = :coupleId ORDER BY a.sortOrder ASC NULLS LAST, a.id DESC",
           countQuery = "SELECT COUNT(a) FROM Album a WHERE a.couple.id = :coupleId")
    Page<Album> findAllByCoupleIdOrdered(@Param("coupleId") Long coupleId, Pageable pageable);

    @Query("SELECT MIN(a.sortOrder) FROM Album a WHERE a.couple.id = :coupleId")
    Integer findMinSortOrderByCoupleId(@Param("coupleId") Long coupleId);
}