package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.PluginEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PluginRepository extends JpaRepository<PluginEntity, String> {
}
