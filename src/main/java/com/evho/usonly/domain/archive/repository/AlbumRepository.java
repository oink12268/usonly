package com.evho.usonly.domain.archive.repository;

import com.evho.usonly.domain.archive.model.Album;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlbumRepository extends JpaRepository<Album, Long> {
    // 필요한 쿼리 메서드가 있다면 여기에 추가 (예: 내 앨범만 보기)
     List<Album> findAllByCoupleId(Long coupleId);
}