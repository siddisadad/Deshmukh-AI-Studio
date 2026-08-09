package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.ContactInquiryEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactInquiryRepository extends JpaRepository<ContactInquiryEntity, UUID> {
    long countByEmailIgnoreCaseAndCreatedAtAfter(String email, Instant createdAt);

    List<ContactInquiryEntity> findAllByOrderByCreatedAtDesc();

    long countByReadAtIsNull();

    @Modifying(clearAutomatically = true)
    @Query("update ContactInquiryEntity c set c.readAt = :readAt where c.readAt is null")
    int markAllUnreadAsRead(@Param("readAt") Instant readAt);
}
