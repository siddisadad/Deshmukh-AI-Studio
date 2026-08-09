package com.aistudio.application.plugin;

import com.aistudio.api.plugin.dto.OrgPluginPackResponse;
import com.aistudio.api.plugin.dto.PluginPackResponse;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.persistence.entity.OrganizationPluginEntity;
import com.aistudio.infrastructure.persistence.entity.OrganizationPluginPackEntity;
import com.aistudio.infrastructure.persistence.entity.PluginPackEntity;
import com.aistudio.infrastructure.persistence.entity.PluginPackMemberEntity;
import com.aistudio.infrastructure.persistence.repository.OrganizationPluginPackRepository;
import com.aistudio.infrastructure.persistence.repository.OrganizationPluginRepository;
import com.aistudio.infrastructure.persistence.repository.PluginPackMemberRepository;
import com.aistudio.infrastructure.persistence.repository.PluginPackRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PluginPackService {

    private final PluginPackRepository packRepository;
    private final PluginPackMemberRepository memberRepository;
    private final OrganizationPluginPackRepository organizationPackRepository;
    private final OrganizationPluginRepository organizationPluginRepository;
    private final ProjectAuthorizationService authorizationService;

    public PluginPackService(
            PluginPackRepository packRepository,
            PluginPackMemberRepository memberRepository,
            OrganizationPluginPackRepository organizationPackRepository,
            OrganizationPluginRepository organizationPluginRepository,
            ProjectAuthorizationService authorizationService
    ) {
        this.packRepository = packRepository;
        this.memberRepository = memberRepository;
        this.organizationPackRepository = organizationPackRepository;
        this.organizationPluginRepository = organizationPluginRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<PluginPackResponse> listMarketplace() {
        return packRepository.findAllByOrderByNameAsc().stream()
                .map(this::toPackResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrgPluginPackResponse> listForOrganization(UUID organizationId, UUID userId) {
        authorizationService.requireOrgMember(organizationId, userId);
        Map<String, OrganizationPluginPackEntity> installed = organizationPackRepository
                .findByOrganizationId(organizationId).stream()
                .collect(Collectors.toMap(OrganizationPluginPackEntity::getPackId, e -> e, (a, b) -> a));
        return packRepository.findAllByOrderByNameAsc().stream()
                .map(pack -> {
                    OrganizationPluginPackEntity row = installed.get(pack.getId());
                    return new OrgPluginPackResponse(
                            toPackResponse(pack),
                            row != null,
                            row == null ? null : row.getInstalledAt()
                    );
                })
                .toList();
    }

    @Transactional
    public OrgPluginPackResponse install(UUID organizationId, UUID userId, String packId) {
        authorizationService.requireOrgOwner(organizationId, userId);
        PluginPackEntity pack = packRepository.findById(packId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Plugin pack not found"));
        if (organizationPackRepository.existsByOrganizationIdAndPackId(organizationId, packId)) {
            OrganizationPluginPackEntity existing = organizationPackRepository
                    .findByOrganizationId(organizationId).stream()
                    .filter(row -> row.getPackId().equals(packId))
                    .findFirst()
                    .orElseThrow();
            return new OrgPluginPackResponse(toPackResponse(pack), true, existing.getInstalledAt());
        }
        OrganizationPluginPackEntity install = new OrganizationPluginPackEntity();
        install.setOrganizationId(organizationId);
        install.setPackId(packId);
        organizationPackRepository.save(install);
        for (PluginPackMemberEntity member : memberRepository.findByPackId(packId)) {
            OrganizationPluginEntity pluginRow = organizationPluginRepository
                    .findByOrganizationIdAndPluginId(organizationId, member.getPluginId())
                    .orElseGet(() -> {
                        OrganizationPluginEntity created = new OrganizationPluginEntity();
                        created.setOrganizationId(organizationId);
                        created.setPluginId(member.getPluginId());
                        return created;
                    });
            pluginRow.setEnabled(true);
            organizationPluginRepository.save(pluginRow);
        }
        return new OrgPluginPackResponse(toPackResponse(pack), true, install.getInstalledAt());
    }

    @Transactional
    public void uninstall(UUID organizationId, UUID userId, String packId) {
        authorizationService.requireOrgOwner(organizationId, userId);
        if (!packRepository.existsById(packId)) {
            throw new DomainException("NOT_FOUND", "Plugin pack not found");
        }
        OrganizationPluginPackEntity.Pk pk = new OrganizationPluginPackEntity.Pk(organizationId, packId);
        if (!organizationPackRepository.existsById(pk)) {
            return;
        }
        organizationPackRepository.deleteById(pk);
        for (PluginPackMemberEntity member : memberRepository.findByPackId(packId)) {
            organizationPluginRepository
                    .findByOrganizationIdAndPluginId(organizationId, member.getPluginId())
                    .ifPresent(organizationPluginRepository::delete);
        }
    }

    @Transactional(readOnly = true)
    public boolean isPackInstalled(UUID organizationId, String packId) {
        return organizationPackRepository.existsByOrganizationIdAndPackId(organizationId, packId);
    }

    @Transactional(readOnly = true)
    public String findPackIdForPlugin(String pluginId) {
        return memberRepository.findByPluginId(pluginId)
                .map(PluginPackMemberEntity::getPackId)
                .orElse(null);
    }

    private PluginPackResponse toPackResponse(PluginPackEntity pack) {
        List<String> pluginIds = memberRepository.findByPackId(pack.getId()).stream()
                .map(PluginPackMemberEntity::getPluginId)
                .sorted()
                .toList();
        return new PluginPackResponse(
                pack.getId(),
                pack.getSlug(),
                pack.getName(),
                pack.getPublisher(),
                pack.getVersion(),
                pack.getDescription(),
                pack.isVerified(),
                pluginIds
        );
    }
}
