package com.evho.usonly.domain.archive.repository;

import com.evho.usonly.domain.archive.model.Album;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlbumRepository extends JpaRepository<Album, Long> {
    List<Album> findAllByCoupleId(Long coupleId);
    Page<Album> findAllByCoupleIdOrderByIdDesc(Long coupleId, Pageable pageable);
}