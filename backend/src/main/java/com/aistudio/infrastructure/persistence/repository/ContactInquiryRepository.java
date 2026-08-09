package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.ContactInquiryEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactInquiryRepository extends JpaRepository<ContactInquiryEntity, UUID> {
    long countByEmailIgnoreCaseAndCreatedAtAfter(String email, Instant createdAt);
}
