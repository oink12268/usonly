package com.evho.usonly.domain.archive.repository;

import com.evho.usonly.domain.archive.model.Media;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<Media, Long> {
}
