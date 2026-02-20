package com.evho.usonly.domain.couple.repository;

import com.evho.usonly.domain.couple.model.Couple;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoupleRepository extends JpaRepository<Couple, Long> {
}