package com.aistudio.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aistudio.api.auth.dto.TokenResponse;
import com.aistudio.application.audit.AuditService;
import com.aistudio.application.billing.BillingService;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.config.JwtProperties;
import com.aistudio.infrastructure.mail.EmailPort;
import com.aistudio.infrastructure.persistence.entity.OrganizationEntity;
import com.aistudio.infrastructure.persistence.entity.UserEntity;
import com.aistudio.infrastructure.persistence.repository.MembershipRepository;
import com.aistudio.infrastructure.persistence.repository.OrganizationRepository;
import com.aistudio.infrastructure.persistence.repository.PasswordResetTokenRepository;
import com.aistudio.infrastructure.persistence.repository.RefreshTokenRepository;
import com.aistudio.infrastructure.persistence.repository.UserIdentityRepository;
import com.aistudio.infrastructure.persistence.repository.UserRepository;
import com.aistudio.infrastructure.security.JwtService;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock MembershipRepository membershipRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock UserIdentityRepository userIdentityRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock EmailPort emailPort;
    @Mock AuditService auditService;
    @Mock BillingService billingService;

    JwtProperties jwtProperties = new JwtProperties("dev-only-change-me-please-use-a-long-random-secret-key", Duration.ofMinutes(15), Duration.ofDays(7));
    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                organizationRepository,
                membershipRepository,
                refreshTokenRepository,
                passwordResetTokenRepository,
                userIdentityRepository,
                passwordEncoder,
                jwtService,
                jwtProperties,
                emailPort,
                auditService,
                billingService,
                "http://localhost:5173"
        );
    }

    @Test
    void registerCreatesUserOrgAndTokens() {
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Str0ngPass!")).thenReturn("hash");
        when(organizationRepository.existsBySlug(anyString())).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(organizationRepository.save(any(OrganizationEntity.class))).thenAnswer(inv -> {
            OrganizationEntity o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });
        when(jwtService.createAccessToken(any(), eq("ada@example.com"))).thenReturn("access");
        when(jwtService.accessTokenTtlSeconds()).thenReturn(900L);

        TokenResponse response = authService.register("Ada@Example.com", "Str0ngPass!", "Ada", "127.0.0.1");

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.user().email()).isEqualTo("ada@example.com");
        verify(membershipRepository).save(any());
        verify(billingService).ensureFreeSubscription(any());
        verify(refreshTokenRepository).save(any());
        verify(auditService).record(any(), eq("USER_REGISTERED"), eq("USER"), any(), anyString(), eq("127.0.0.1"));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);
        assertThatThrownBy(() -> authService.register("ada@example.com", "Str0ngPass!", "Ada", "127.0.0.1"))
                .isInstanceOf(DomainException.class)
                .extracting("code")
                .isEqualTo("EMAIL_TAKEN");
    }

    @Test
    void loginRejectsBadPassword() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("ada@example.com");
        user.setPasswordHash("hash");
        when(userRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("ada@example.com", "wrong", "127.0.0.1"))
                .isInstanceOf(DomainException.class)
                .extracting("code")
                .isEqualTo("INVALID_CREDENTIALS");

        ArgumentCaptor<String> action = ArgumentCaptor.forClass(String.class);
        verify(auditService).record(any(), action.capture(), any(), any(), any(), any());
        assertThat(action.getValue()).isEqualTo("LOGIN_FAILED");
    }
}
