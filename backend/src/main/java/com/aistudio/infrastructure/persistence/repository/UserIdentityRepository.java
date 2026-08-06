package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.UserIdentityEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserIdentityRepository extends JpaRepository<UserIdentityEntity, UUID> {

    Optional<UserIdentityEntity> findByProviderAndSubject(String provider, String subject);
}
