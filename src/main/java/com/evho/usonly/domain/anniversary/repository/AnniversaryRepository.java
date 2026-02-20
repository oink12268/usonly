package com.evho.usonly.domain.anniversary.repository;

import com.evho.usonly.domain.anniversary.model.Anniversary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnniversaryRepository extends JpaRepository<Anniversary, Long> {
    List<Anniversary> findAllByCoupleIdOrderByDateAsc(Long coupleId);
}
