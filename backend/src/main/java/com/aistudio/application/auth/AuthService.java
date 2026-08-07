package com.aistudio.application.auth;

import com.aistudio.api.auth.dto.TokenResponse;
import com.aistudio.api.profile.dto.ChangePasswordRequest;
import com.aistudio.api.profile.dto.MeResponse;
import com.aistudio.api.profile.dto.UpdateProfileRequest;
import com.aistudio.application.audit.AuditService;
import com.aistudio.application.billing.BillingService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.domain.organization.OrgRole;
import com.aistudio.domain.user.ThemePreference;
import com.aistudio.infrastructure.config.JwtProperties;
import com.aistudio.infrastructure.mail.EmailPort;
import com.aistudio.infrastructure.persistence.entity.MembershipEntity;
import com.aistudio.infrastructure.persistence.entity.OrganizationEntity;
import com.aistudio.infrastructure.persistence.entity.PasswordResetTokenEntity;
import com.aistudio.infrastructure.persistence.entity.RefreshTokenEntity;
import com.aistudio.infrastructure.persistence.entity.UserEntity;
import com.aistudio.infrastructure.persistence.entity.UserIdentityEntity;
import com.aistudio.infrastructure.persistence.repository.MembershipRepository;
import com.aistudio.infrastructure.persistence.repository.OrganizationRepository;
import com.aistudio.infrastructure.persistence.repository.PasswordResetTokenRepository;
import com.aistudio.infrastructure.persistence.repository.RefreshTokenRepository;
import com.aistudio.infrastructure.persistence.repository.UserIdentityRepository;
import com.aistudio.infrastructure.persistence.repository.UserRepository;
import com.aistudio.infrastructure.security.JwtService;
import com.aistudio.shared.util.SlugUtils;
import com.aistudio.shared.util.TokenHashUtils;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final EmailPort emailPort;
    private final AuditService auditService;
    private final BillingService billingService;
    private final String appBaseUrl;

    public AuthService(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            MembershipRepository membershipRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            UserIdentityRepository userIdentityRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            EmailPort emailPort,
            AuditService auditService,
            BillingService billingService,
            @Value("${aistudio.billing.app-base-url:http://localhost:5173}") String appBaseUrl
    ) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.emailPort = emailPort;
        this.auditService = auditService;
        this.billingService = billingService;
        this.appBaseUrl = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
    }

    @Transactional
    public TokenResponse register(String email, String password, String displayName, String ip) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DomainException("EMAIL_TAKEN", "Email is already registered");
        }

        UserEntity user = new UserEntity();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(displayName.trim());
        user.setTheme(ThemePreference.SYSTEM);
        user.setEmailVerified(false);
        userRepository.save(user);

        OrganizationEntity org = new OrganizationEntity();
        org.setName(displayName.trim() + "'s Workspace");
        org.setSlug(uniqueSlug(SlugUtils.slugify(displayName.trim() + "-workspace")));
        organizationRepository.save(org);

        MembershipEntity membership = new MembershipEntity();
        membership.setOrganizationId(org.getId());
        membership.setUserId(user.getId());
        membership.setRole(OrgRole.OWNER);
        membershipRepository.save(membership);

        billingService.ensureFreeSubscription(org.getId());

        auditService.record(user.getId(), "USER_REGISTERED", "USER", user.getId(), "{}", ip);
        return issueTokens(user, org);
    }

    @Transactional
    public TokenResponse login(String email, String password, String ip) {
        UserEntity user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> invalidCredentials());
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new DomainException("INVALID_CREDENTIALS", "This account uses SSO. Sign in with SSO instead.");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            auditService.record(null, "LOGIN_FAILED", "USER", null, "{\"email\":\"" + normalizeEmail(email) + "\"}", ip);
            throw invalidCredentials();
        }
        OrganizationEntity org = primaryOrganization(user.getId());
        auditService.record(user.getId(), "LOGIN_SUCCESS", "USER", user.getId(), "{}", ip);
        return issueTokens(user, org);
    }

    @Transactional
    public TokenResponse loginWithExternalIdentity(
            String provider,
            String subject,
            String email,
            String displayName,
            boolean emailVerified,
            String ip
    ) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) {
            throw new DomainException("VALIDATION_ERROR", "SSO provider did not return a valid email");
        }

        UserEntity user = userIdentityRepository.findByProviderAndSubject(provider, subject)
                .flatMap(identity -> userRepository.findById(identity.getUserId()))
                .orElseGet(() -> userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null));

        if (user == null) {
            user = new UserEntity();
            user.setEmail(normalizedEmail);
            user.setPasswordHash(null);
            user.setDisplayName(displayName == null || displayName.isBlank()
                    ? normalizedEmail.substring(0, normalizedEmail.indexOf('@'))
                    : displayName.trim());
            user.setTheme(ThemePreference.SYSTEM);
            user.setEmailVerified(emailVerified);
            userRepository.save(user);

            OrganizationEntity org = new OrganizationEntity();
            org.setName(user.getDisplayName() + "'s Workspace");
            org.setSlug(uniqueSlug(SlugUtils.slugify(user.getDisplayName() + "-workspace")));
            organizationRepository.save(org);

            MembershipEntity membership = new MembershipEntity();
            membership.setOrganizationId(org.getId());
            membership.setUserId(user.getId());
            membership.setRole(OrgRole.OWNER);
            membershipRepository.save(membership);

            billingService.ensureFreeSubscription(org.getId());
            auditService.record(user.getId(), "USER_REGISTERED_SSO", "USER", user.getId(),
                    "{\"provider\":\"" + provider + "\"}", ip);
        } else if (emailVerified && !user.isEmailVerified()) {
            user.setEmailVerified(true);
            userRepository.save(user);
        }

        final UUID userId = user.getId();
        if (userIdentityRepository.findByProviderAndSubject(provider, subject).isEmpty()) {
            UserIdentityEntity identity = new UserIdentityEntity();
            identity.setUserId(userId);
            identity.setProvider(provider);
            identity.setSubject(subject);
            identity.setEmail(normalizedEmail);
            userIdentityRepository.save(identity);
        }

        OrganizationEntity org = primaryOrganization(userId);
        auditService.record(userId, "LOGIN_SUCCESS_SSO", "USER", userId,
                "{\"provider\":\"" + provider + "\"}", ip);
        return issueTokens(user, org);
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        RefreshTokenEntity stored = refreshTokenRepository.findByTokenHash(TokenHashUtils.sha256(refreshToken))
                .orElseThrow(() -> new DomainException("INVALID_TOKEN", "Refresh token is invalid"));
        if (!stored.isActive()) {
            throw new DomainException("INVALID_TOKEN", "Refresh token is invalid or expired");
        }
        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        UserEntity user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new DomainException("INVALID_TOKEN", "Refresh token is invalid"));
        OrganizationEntity org = primaryOrganization(user.getId());
        return issueTokens(user, org);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(TokenHashUtils.sha256(refreshToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            }
        });
    }

    @Transactional
    public void forgotPassword(String email, String ip) {
        userRepository.findByEmailIgnoreCase(normalizeEmail(email)).ifPresent(user -> {
            String rawToken = TokenHashUtils.generateOpaqueToken();
            PasswordResetTokenEntity reset = new PasswordResetTokenEntity();
            reset.setUserId(user.getId());
            reset.setTokenHash(TokenHashUtils.sha256(rawToken));
            reset.setExpiresAt(Instant.now().plusSeconds(3600));
            passwordResetTokenRepository.save(reset);
            String resetUrl = appBaseUrl + "/reset-password?token="
                    + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
            emailPort.send(
                    user.getEmail(),
                    "AI Studio password reset",
                    """
                    Reset your AI Studio password (link valid 1 hour):
                    %s

                    If the link does not open, paste this token on the reset page:
                    %s
                    """.formatted(resetUrl, rawToken)
            );
            auditService.record(user.getId(), "PASSWORD_RESET_REQUESTED", "USER", user.getId(), "{}", ip);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword, String ip) {
        PasswordResetTokenEntity reset = passwordResetTokenRepository.findByTokenHash(TokenHashUtils.sha256(token))
                .orElseThrow(() -> new DomainException("TOKEN_INVALID_OR_EXPIRED", "Reset token is invalid or expired"));
        if (!reset.isUsable()) {
            throw new DomainException("TOKEN_INVALID_OR_EXPIRED", "Reset token is invalid or expired");
        }
        UserEntity user = userRepository.findById(reset.getUserId())
                .orElseThrow(() -> new DomainException("TOKEN_INVALID_OR_EXPIRED", "Reset token is invalid or expired"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        Instant now = Instant.now();
        for (PasswordResetTokenEntity outstanding : passwordResetTokenRepository.findByUserIdAndUsedAtIsNull(user.getId())) {
            outstanding.setUsedAt(now);
            passwordResetTokenRepository.save(outstanding);
        }
        for (RefreshTokenEntity refresh : refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId())) {
            refresh.setRevokedAt(now);
            refreshTokenRepository.save(refresh);
        }
        auditService.record(user.getId(), "PASSWORD_RESET_COMPLETED", "USER", user.getId(), "{}", ip);
    }

    @Transactional(readOnly = true)
    public MeResponse me(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "User not found"));
        List<MeResponse.OrgMembership> orgs = membershipRepository.findByUserId(userId).stream()
                .map(m -> {
                    OrganizationEntity org = organizationRepository.findById(m.getOrganizationId())
                            .orElseThrow(() -> new DomainException("NOT_FOUND", "Organization not found"));
                    return new MeResponse.OrgMembership(org.getId(), org.getName(), org.getSlug(), m.getRole().name());
                })
                .toList();
        return new MeResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getTheme().name(), orgs);
    }

    @Transactional
    public MeResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "User not found"));
        if (request.displayName() != null && !request.displayName().isBlank()) {
            user.setDisplayName(request.displayName().trim());
        }
        if (request.theme() != null) {
            user.setTheme(request.theme());
        }
        userRepository.save(user);
        return me(userId);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "User not found"));
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "This account uses SSO and has no local password");
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new DomainException("INVALID_CREDENTIALS", "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        auditService.record(userId, "PASSWORD_CHANGED", "USER", userId, "{}", null);
    }

    private TokenResponse issueTokens(UserEntity user, OrganizationEntity org) {
        String accessToken = jwtService.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = TokenHashUtils.generateOpaqueToken();

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(user.getId());
        entity.setTokenHash(TokenHashUtils.sha256(refreshToken));
        entity.setExpiresAt(Instant.now().plus(jwtProperties.refreshTokenTtl()));
        refreshTokenRepository.save(entity);

        return new TokenResponse(
                new TokenResponse.UserResponse(
                        user.getId(), user.getEmail(), user.getDisplayName(), user.getTheme().name()),
                new TokenResponse.OrganizationResponse(org.getId(), org.getName(), org.getSlug()),
                accessToken,
                refreshToken,
                jwtService.accessTokenTtlSeconds()
        );
    }

    private OrganizationEntity primaryOrganization(UUID userId) {
        return membershipRepository.findByUserId(userId).stream()
                .findFirst()
                .flatMap(m -> organizationRepository.findById(m.getOrganizationId()))
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Organization not found"));
    }

    private String uniqueSlug(String base) {
        String candidate = base;
        int i = 1;
        while (organizationRepository.existsBySlug(candidate)) {
            candidate = base + "-" + i++;
        }
        return candidate;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static DomainException invalidCredentials() {
        return new DomainException("INVALID_CREDENTIALS", "Invalid email or password");
    }
}
