package com.aistudio.infrastructure.persistence.repository;

import com.aistudio.infrastructure.persistence.entity.PluginPackMemberEntity;
import com.aistudio.infrastructure.persistence.entity.PluginPackMemberEntity.Pk;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PluginPackMemberRepository extends JpaRepository<PluginPackMemberEntity, Pk> {

    List<PluginPackMemberEntity> findByPackId(String packId);

    Optional<PluginPackMemberEntity> findByPluginId(String pluginId);
}
