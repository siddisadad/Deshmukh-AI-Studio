package com.aistudio.application.plugin;

import com.aistudio.infrastructure.persistence.entity.PluginPackEntity;
import com.aistudio.infrastructure.persistence.entity.PluginPackMemberEntity;
import com.aistudio.infrastructure.persistence.repository.PluginPackMemberRepository;
import com.aistudio.infrastructure.persistence.repository.PluginPackRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PluginPackCatalogSync {

    private final PluginPackManifestLoader manifestLoader;
    private final PluginPackRepository packRepository;
    private final PluginPackMemberRepository memberRepository;
    private final PluginRegistry pluginRegistry;

    public PluginPackCatalogSync(
            PluginPackManifestLoader manifestLoader,
            PluginPackRepository packRepository,
            PluginPackMemberRepository memberRepository,
            PluginRegistry pluginRegistry
    ) {
        this.manifestLoader = manifestLoader;
        this.packRepository = packRepository;
        this.memberRepository = memberRepository;
        this.pluginRegistry = pluginRegistry;
    }

    @Transactional
    public void syncFromManifests() {
        Set<String> seenMembers = new HashSet<>();
        for (PluginPackManifest manifest : manifestLoader.loadAll()) {
            PluginPackEntity pack = packRepository.findById(manifest.id()).orElseGet(PluginPackEntity::new);
            pack.setId(manifest.id());
            pack.setSlug(manifest.slug());
            pack.setName(manifest.name());
            pack.setPublisher(manifest.publisher());
            pack.setVersion(manifest.version());
            pack.setDescription(manifest.description() == null ? "" : manifest.description());
            pack.setVerified(manifest.verified());
            packRepository.save(pack);

            if (manifest.pluginIds() == null) {
                continue;
            }
            for (String pluginId : manifest.pluginIds()) {
                pluginRegistry.require(pluginId);
                PluginPackMemberEntity.Pk pk = new PluginPackMemberEntity.Pk(manifest.id(), pluginId);
                if (!memberRepository.existsById(pk)) {
                    PluginPackMemberEntity member = new PluginPackMemberEntity();
                    member.setPackId(manifest.id());
                    member.setPluginId(pluginId);
                    memberRepository.save(member);
                }
                seenMembers.add(manifest.id() + ":" + pluginId);
            }
        }
        if (seenMembers.isEmpty()) {
            return;
        }
        List<PluginPackMemberEntity> existing = memberRepository.findAll();
        for (PluginPackMemberEntity member : existing) {
            String key = member.getPackId() + ":" + member.getPluginId();
            if (!seenMembers.contains(key)) {
                memberRepository.delete(member);
            }
        }
    }
}
