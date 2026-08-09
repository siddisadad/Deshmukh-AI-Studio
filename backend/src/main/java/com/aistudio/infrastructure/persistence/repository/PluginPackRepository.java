package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.PluginPackEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PluginPackRepository extends JpaRepository<PluginPackEntity, String> {

    List<PluginPackEntity> findAllByOrderByNameAsc();
}
