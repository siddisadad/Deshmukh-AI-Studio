package com.aistudio.application.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aistudio.domain.billing.SubscriptionStatus;
import com.aistudio.infrastructure.config.BillingProperties;
import com.aistudio.infrastructure.mail.EmailPort;
import com.aistudio.infrastructure.persistence.entity.OrganizationSubscriptionEntity;
import com.aistudio.infrastructure.persistence.repository.BillingDunningEventRepository;
import com.aistudio.infrastructure.persistence.repository.MembershipRepository;
import com.aistudio.infrastructure.persistence.repository.OrganizationRepository;
import com.aistudio.infrastructure.persistence.repository.OrganizationSubscriptionRepository;
import com.aistudio.infrastructure.persistence.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BillingDunningServiceTest {

    @Mock BillingProperties billingProperties;
    @Mock OrganizationSubscriptionRepository subscriptionRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock MembershipRepository membershipRepository;
    @Mock UserRepository userRepository;
    @Mock BillingDunningEventRepository eventRepository;
    @Mock EmailPort emailPort;

    BillingDunningService service;
    UUID organizationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new BillingDunningService(
                billingProperties,
                subscriptionRepository,
                organizationRepository,
                membershipRepository,
                userRepository,
                eventRepository,
                emailPort
        );
    }

    @Test
    void handlePaymentSucceededClearsDunningState() {
        OrganizationSubscriptionEntity sub = new OrganizationSubscriptionEntity();
        sub.setOrganizationId(organizationId);
        sub.setStatus(SubscriptionStatus.PAST_DUE);
        sub.setDunningStage(2);
        when(subscriptionRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.handlePaymentSucceeded(organizationId);

        assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus());
        assertEquals(0, sub.getDunningStage());
        assertNull(sub.getDunningLastNotifiedAt());
        verify(subscriptionRepository).save(sub);
        verify(eventRepository).save(any());
    }

    @Test
    void handlePaymentFailedSetsPastDueAndInitialStage() {
        OrganizationSubscriptionEntity sub = new OrganizationSubscriptionEntity();
        sub.setOrganizationId(organizationId);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setDunningStage(0);
        when(subscriptionRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(membershipRepository.findByOrganizationIdOrderByCreatedAtAsc(organizationId)).thenReturn(List.of());

        service.handlePaymentFailed(organizationId, "in_123");

        assertEquals(SubscriptionStatus.PAST_DUE, sub.getStatus());
        assertEquals(1, sub.getDunningStage());
        verify(subscriptionRepository).save(sub);
        verify(eventRepository).save(any());
    }
}
