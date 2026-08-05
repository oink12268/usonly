package com.evho.usonly.domain.couple.repository;

import com.evho.usonly.domain.couple.entity.Couple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CoupleRepository extends JpaRepository<Couple, Long> {

    // 파일 저장/삭제 시 용량 카운터를 원자적으로 증감. delta는 음수(삭제)도 허용.
    @Modifying
    @Transactional
    @Query("UPDATE Couple c SET c.storageUsedBytes = COALESCE(c.storageUsedBytes, 0) + :delta WHERE c.id = :coupleId")
    void addStorageUsedBytes(@Param("coupleId") Long coupleId, @Param("delta") long delta);
}