package com.aistudio.application.billing;

import com.aistudio.domain.billing.SubscriptionStatus;
import com.aistudio.domain.organization.OrgRole;
import com.aistudio.infrastructure.config.BillingProperties;
import com.aistudio.infrastructure.mail.EmailPort;
import com.aistudio.infrastructure.persistence.entity.BillingDunningEventEntity;
import com.aistudio.infrastructure.persistence.entity.MembershipEntity;
import com.aistudio.infrastructure.persistence.entity.OrganizationEntity;
import com.aistudio.infrastructure.persistence.entity.OrganizationSubscriptionEntity;
import com.aistudio.infrastructure.persistence.entity.UserEntity;
import com.aistudio.infrastructure.persistence.repository.BillingDunningEventRepository;
import com.aistudio.infrastructure.persistence.repository.MembershipRepository;
import com.aistudio.infrastructure.persistence.repository.OrganizationRepository;
import com.aistudio.infrastructure.persistence.repository.OrganizationSubscriptionRepository;
import com.aistudio.infrastructure.persistence.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingDunningService {

    private final BillingProperties billingProperties;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final BillingDunningEventRepository eventRepository;
    private final EmailPort emailPort;

    public BillingDunningService(
            BillingProperties billingProperties,
            OrganizationSubscriptionRepository subscriptionRepository,
            OrganizationRepository organizationRepository,
            MembershipRepository membershipRepository,
            UserRepository userRepository,
            BillingDunningEventRepository eventRepository,
            EmailPort emailPort
    ) {
        this.billingProperties = billingProperties;
        this.subscriptionRepository = subscriptionRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.emailPort = emailPort;
    }

    @Transactional
    public void handlePaymentFailed(UUID organizationId, String invoiceId) {
        OrganizationSubscriptionEntity sub = subscriptionRepository.findByOrganizationId(organizationId)
                .orElse(null);
        if (sub == null) {
            return;
        }
        sub.setStatus(SubscriptionStatus.PAST_DUE);
        if (sub.getDunningStage() < 1) {
            sub.setDunningStage(1);
        }
        subscriptionRepository.save(sub);
        notifyOwners(organizationId, sub.getDunningStage(), "payment_failed", invoiceId);
    }

    @Transactional
    public void handlePaymentSucceeded(UUID organizationId) {
        OrganizationSubscriptionEntity sub = subscriptionRepository.findByOrganizationId(organizationId)
                .orElse(null);
        if (sub == null) {
            return;
        }
        if (sub.getStatus() == SubscriptionStatus.PAST_DUE || sub.getDunningStage() > 0) {
            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.setDunningStage(0);
            sub.setDunningLastNotifiedAt(null);
            subscriptionRepository.save(sub);
            recordEvent(organizationId, "payment_succeeded", 0, false,
                    "Payment succeeded — dunning cleared");
        }
    }

    @Transactional
    public BillingDunningRunResult runScheduledDunning() {
        if (!billingProperties.dunningEnabled()) {
            return new BillingDunningRunResult(0, 0, 0, List.of("Billing dunning is disabled"));
        }
        List<OrganizationSubscriptionEntity> pastDue = subscriptionRepository.findAll().stream()
                .filter(sub -> sub.getStatus() == SubscriptionStatus.PAST_DUE)
                .toList();
        int processed = 0;
        int notified = 0;
        int skipped = 0;
        List<String> messages = new ArrayList<>();
        Instant now = Instant.now();
        long intervalHours = billingProperties.dunningIntervalHours();
        int maxStage = billingProperties.dunningMaxStage();
        for (OrganizationSubscriptionEntity sub : pastDue) {
            processed++;
            UUID organizationId = sub.getOrganizationId();
            if (sub.getDunningLastNotifiedAt() != null
                    && sub.getDunningLastNotifiedAt().isAfter(now.minus(intervalHours, ChronoUnit.HOURS))) {
                skipped++;
                messages.add(organizationId + ": skip — within dunning interval");
                continue;
            }
            int nextStage = Math.min(maxStage, sub.getDunningStage() + 1);
            if (sub.getDunningStage() >= maxStage && sub.getDunningLastNotifiedAt() != null) {
                skipped++;
                messages.add(organizationId + ": skip — max dunning stage reached");
                continue;
            }
            sub.setDunningStage(nextStage);
            sub.setDunningLastNotifiedAt(now);
            subscriptionRepository.save(sub);
            notifyOwners(organizationId, nextStage, "scheduled_reminder", null);
            notified++;
            messages.add(organizationId + ": dunning stage " + nextStage);
        }
        return new BillingDunningRunResult(processed, notified, skipped, messages);
    }

    private void notifyOwners(
            UUID organizationId,
            int stage,
            String eventType,
            String detail
    ) {
        OrganizationEntity org = organizationRepository.findById(organizationId).orElse(null);
        String orgName = org != null ? org.getName() : organizationId.toString();
        List<MembershipEntity> memberships = membershipRepository.findByOrganizationIdOrderByCreatedAtAsc(organizationId);
        boolean emailSent = false;
        for (MembershipEntity membership : memberships) {
            if (membership.getRole() != OrgRole.OWNER) {
                continue;
            }
            UserEntity user = userRepository.findById(membership.getUserId()).orElse(null);
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                continue;
            }
            String subject = stage >= billingProperties.dunningMaxStage()
                    ? "Urgent: update billing for " + orgName
                    : "Payment failed for " + orgName;
            String body = "Your organization \"" + orgName + "\" has a past-due subscription.\n\n"
                    + "Dunning stage: " + stage + " of " + billingProperties.dunningMaxStage() + ".\n"
                    + "Update your payment method in Billing settings to avoid service interruption.\n";
            if (detail != null && !detail.isBlank()) {
                body += "\nReference: " + detail + "\n";
            }
            emailPort.send(user.getEmail(), subject, body);
            emailSent = true;
        }
        recordEvent(organizationId, eventType, stage, emailSent, detail);
    }

    private void recordEvent(
            UUID organizationId,
            String eventType,
            int stage,
            boolean emailSent,
            String detail
    ) {
        BillingDunningEventEntity event = new BillingDunningEventEntity();
        event.setOrganizationId(organizationId);
        event.setEventType(eventType);
        event.setDunningStage(stage);
        event.setEmailSent(emailSent);
        if (detail != null && detail.length() > 512) {
            event.setDetail(detail.substring(0, 512));
        } else {
            event.setDetail(detail);
        }
        eventRepository.save(event);
    }

    public record BillingDunningRunResult(int processed, int notified, int skipped, List<String> messages) {
    }
}
