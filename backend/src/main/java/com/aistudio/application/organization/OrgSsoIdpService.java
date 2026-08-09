package com.aistudio.application.organization;

import com.aistudio.api.organization.dto.CreateOrgSsoIdpRequest;
import com.aistudio.api.organization.dto.OrgSsoIdpResponse;
import com.aistudio.api.organization.dto.UpdateOrgSsoIdpRequest;
import com.aistudio.application.auth.SsoMetadataRefreshService;
import com.aistudio.application.security.ProjectAuthorizationService;
import com.aistudio.domain.auth.OrgSsoIdpProtocol;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.persistence.entity.OrgSsoIdpEntity;
import com.aistudio.infrastructure.persistence.repository.OrgSsoIdpRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrgSsoIdpService {

    private final OrgSsoIdpRepository idpRepository;
    private final ProjectAuthorizationService authorizationService;
    private final SsoMetadataRefreshService metadataRefreshService;

    public OrgSsoIdpService(
            OrgSsoIdpRepository idpRepository,
            ProjectAuthorizationService authorizationService,
            SsoMetadataRefreshService metadataRefreshService
    ) {
        this.idpRepository = idpRepository;
        this.authorizationService = authorizationService;
        this.metadataRefreshService = metadataRefreshService;
    }

    @Transactional(readOnly = true)
    public List<OrgSsoIdpResponse> list(UUID organizationId, UUID userId) {
        authorizationService.requireOrgMember(organizationId, userId);
        return idpRepository.findByOrganizationIdOrderByDisplayNameAsc(organizationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrgSsoIdpResponse create(UUID organizationId, UUID userId, CreateOrgSsoIdpRequest request) {
        authorizationService.requireOrgOwner(organizationId, userId);
        String slug = request.slug().trim().toLowerCase(Locale.ROOT);
        if (idpRepository.findByOrganizationIdAndSlug(organizationId, slug).isPresent()) {
            throw new DomainException("VALIDATION_ERROR", "SSO IdP slug already exists for this organization");
        }
        OrgSsoIdpProtocol protocol = parseProtocol(request.protocol());
        validateCreate(protocol, request);
        OrgSsoIdpEntity idp = new OrgSsoIdpEntity();
        idp.setOrganizationId(organizationId);
        idp.setSlug(slug);
        idp.setProtocol(protocol);
        idp.setDisplayName(request.displayName().trim());
        idp.setEnabled(request.enabled());
        applyOidcFields(idp, request.issuerUri(), request.clientId(), request.clientSecret(), request.scopes());
        applySamlFields(
                idp,
                request.metadataUrl(),
                request.entityId(),
                request.acsUrl(),
                request.spPrivateKey(),
                request.spCertificate(),
                request.wantEncryptedAssertions()
        );
        idp = idpRepository.save(idp);
        try {
            metadataRefreshService.refreshMetadata(idp);
        } catch (DomainException ignored) {
            // metadata refresh errors are stored on entity
        }
        return toResponse(idpRepository.findById(idp.getId()).orElse(idp));
    }

    @Transactional
    public OrgSsoIdpResponse update(
            UUID organizationId,
            UUID idpId,
            UUID userId,
            UpdateOrgSsoIdpRequest request
    ) {
        authorizationService.requireOrgOwner(organizationId, userId);
        OrgSsoIdpEntity idp = requireIdp(organizationId, idpId);
        if (request.displayName() != null && !request.displayName().isBlank()) {
            idp.setDisplayName(request.displayName().trim());
        }
        if (request.enabled() != null) {
            idp.setEnabled(request.enabled());
        }
        if (idp.getProtocol() == OrgSsoIdpProtocol.OIDC) {
            if (request.issuerUri() != null) {
                idp.setIssuerUri(blankToNull(request.issuerUri()));
            }
            if (request.clientId() != null) {
                idp.setClientId(blankToNull(request.clientId()));
            }
            if (request.clientSecret() != null && !request.clientSecret().isBlank()) {
                idp.setClientSecret(request.clientSecret().trim());
            }
            if (request.scopes() != null) {
                idp.setScopes(blankToNull(request.scopes()));
            }
        } else {
            if (request.metadataUrl() != null) {
                idp.setMetadataUrl(blankToNull(request.metadataUrl()));
            }
            if (request.entityId() != null) {
                idp.setEntityId(blankToNull(request.entityId()));
            }
            if (request.acsUrl() != null) {
                idp.setAcsUrl(blankToNull(request.acsUrl()));
            }
            if (request.spPrivateKey() != null) {
                idp.setSpPrivateKey(blankToNull(request.spPrivateKey()));
            }
            if (request.spCertificate() != null) {
                idp.setSpCertificate(blankToNull(request.spCertificate()));
            }
            if (request.wantEncryptedAssertions() != null) {
                idp.setWantEncryptedAssertions(request.wantEncryptedAssertions());
            }
        }
        idp = idpRepository.save(idp);
        return toResponse(idp);
    }

    @Transactional
    public void delete(UUID organizationId, UUID idpId, UUID userId) {
        authorizationService.requireOrgOwner(organizationId, userId);
        OrgSsoIdpEntity idp = requireIdp(organizationId, idpId);
        idpRepository.delete(idp);
    }

    @Transactional
    public OrgSsoIdpResponse refreshMetadata(UUID organizationId, UUID idpId, UUID userId) {
        authorizationService.requireOrgOwner(organizationId, userId);
        OrgSsoIdpEntity idp = requireIdp(organizationId, idpId);
        return toResponse(metadataRefreshService.refreshMetadata(idp));
    }

    private OrgSsoIdpEntity requireIdp(UUID organizationId, UUID idpId) {
        return idpRepository.findByIdAndOrganizationId(idpId, organizationId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "SSO IdP not found"));
    }

    private static OrgSsoIdpProtocol parseProtocol(String raw) {
        try {
            return OrgSsoIdpProtocol.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new DomainException("VALIDATION_ERROR", "protocol must be OIDC or SAML");
        }
    }

    private static void validateCreate(OrgSsoIdpProtocol protocol, CreateOrgSsoIdpRequest request) {
        if (protocol == OrgSsoIdpProtocol.OIDC) {
            if (request.issuerUri() == null || request.issuerUri().isBlank()
                    || request.clientId() == null || request.clientId().isBlank()
                    || request.clientSecret() == null || request.clientSecret().isBlank()) {
                throw new DomainException(
                        "VALIDATION_ERROR",
                        "OIDC issuerUri, clientId, and clientSecret are required"
                );
            }
        } else {
            if (request.metadataUrl() == null || request.metadataUrl().isBlank()
                    || request.entityId() == null || request.entityId().isBlank()
                    || request.acsUrl() == null || request.acsUrl().isBlank()) {
                throw new DomainException(
                        "VALIDATION_ERROR",
                        "SAML metadataUrl, entityId, and acsUrl are required"
                );
            }
        }
    }

    private static void applyOidcFields(
            OrgSsoIdpEntity idp,
            String issuerUri,
            String clientId,
            String clientSecret,
            String scopes
    ) {
        if (idp.getProtocol() != OrgSsoIdpProtocol.OIDC) {
            return;
        }
        idp.setIssuerUri(issuerUri.trim());
        idp.setClientId(clientId.trim());
        idp.setClientSecret(clientSecret.trim());
        idp.setScopes(blankToNull(scopes));
    }

    private static void applySamlFields(
            OrgSsoIdpEntity idp,
            String metadataUrl,
            String entityId,
            String acsUrl,
            String spPrivateKey,
            String spCertificate,
            boolean wantEncrypted
    ) {
        if (idp.getProtocol() != OrgSsoIdpProtocol.SAML) {
            return;
        }
        idp.setMetadataUrl(metadataUrl.trim());
        idp.setEntityId(entityId.trim());
        idp.setAcsUrl(acsUrl.trim());
        idp.setSpPrivateKey(blankToNull(spPrivateKey));
        idp.setSpCertificate(blankToNull(spCertificate));
        idp.setWantEncryptedAssertions(wantEncrypted);
    }

    private OrgSsoIdpResponse toResponse(OrgSsoIdpEntity idp) {
        return new OrgSsoIdpResponse(
                idp.getId(),
                idp.getSlug(),
                idp.getProtocol().name(),
                idp.getDisplayName(),
                idp.isEnabled(),
                idp.getIssuerUri(),
                idp.getClientId(),
                idp.getClientSecret() != null && !idp.getClientSecret().isBlank(),
                idp.getScopes(),
                idp.getMetadataUrl(),
                idp.getEntityId(),
                idp.getAcsUrl(),
                idp.getSpPrivateKey() != null && !idp.getSpPrivateKey().isBlank()
                        && idp.getSpCertificate() != null && !idp.getSpCertificate().isBlank(),
                idp.isWantEncryptedAssertions(),
                idp.getMetadataFetchedAt(),
                idp.getMetadataRefreshError()
        );
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
