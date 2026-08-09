package com.aistudio.infrastructure.billing;

import com.aistudio.application.billing.BillingPort;
import com.aistudio.domain.billing.PlanCode;
import com.aistudio.domain.common.DomainException;
import com.aistudio.infrastructure.config.BillingProperties;
import com.aistudio.infrastructure.persistence.entity.OrganizationSubscriptionEntity;
import com.aistudio.infrastructure.persistence.entity.PlanEntity;
import com.aistudio.infrastructure.persistence.repository.OrganizationSubscriptionRepository;
import com.aistudio.infrastructure.persistence.repository.PlanRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.UsageRecord;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.UsageRecordCreateOnSubscriptionItemParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams.LineItem;
import com.stripe.param.InvoiceListParams;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "aistudio.billing.provider", havingValue = "stripe")
public class StripeBillingAdapter implements BillingPort {

    private final BillingProperties billingProperties;
    private final PlanRepository planRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final String appBaseUrl;

    public StripeBillingAdapter(
            BillingProperties billingProperties,
            PlanRepository planRepository,
            OrganizationSubscriptionRepository subscriptionRepository
    ) {
        this.billingProperties = billingProperties;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        if (!billingProperties.stripe().configured()) {
            throw new IllegalStateException("aistudio.billing.provider=stripe requires STRIPE_API_KEY");
        }
        Stripe.apiKey = billingProperties.stripe().apiKey();
        String base = billingProperties.appBaseUrl();
        this.appBaseUrl = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    @Override
    public String providerId() {
        return "stripe";
    }

    @Override
    public CheckoutSession createCheckoutSession(
            UUID organizationId,
            PlanCode planCode,
            String successUrl,
            String cancelUrl
    ) {
        PlanEntity plan = planRepository.findById(planCode)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Plan not found"));
        String priceId = resolveBasePriceId(plan);
        if (priceId == null || priceId.isBlank()) {
            throw new DomainException(
                    "CONFIG_ERROR",
                    "Stripe price ID not configured for plan " + planCode.name()
            );
        }
        String customerId = ensureCustomer(organizationId);
        String resolvedSuccess = blankToDefault(successUrl, appBaseUrl + "/settings/billing?checkout=success");
        String resolvedCancel = blankToDefault(cancelUrl, appBaseUrl + "/settings/billing?checkout=cancel");

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(customerId)
                .setSuccessUrl(resolvedSuccess)
                .setCancelUrl(resolvedCancel)
                .setClientReferenceId(organizationId.toString())
                .putMetadata("organizationId", organizationId.toString())
                .putMetadata("planCode", planCode.name())
                .addLineItem(LineItem.builder().setPrice(priceId).setQuantity(1L).build());

        String seatMeteredPriceId = resolveSeatMeteredPriceId(plan);
        if (seatMeteredPriceId != null && !seatMeteredPriceId.isBlank()) {
            builder.addLineItem(LineItem.builder().setPrice(seatMeteredPriceId).build());
        }
        String aiOverageMeteredPriceId = resolveAiOverageMeteredPriceId(plan);
        if (aiOverageMeteredPriceId != null && !aiOverageMeteredPriceId.isBlank()) {
            builder.addLineItem(LineItem.builder().setPrice(aiOverageMeteredPriceId).build());
        }

        try {
            com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.create(builder.build());
            return new CheckoutSession(session.getId(), session.getUrl());
        } catch (StripeException ex) {
            throw new DomainException("BILLING_ERROR", "Stripe checkout failed: " + ex.getMessage());
        }
    }

    @Override
    public String createCustomerPortalUrl(UUID organizationId, String returnUrl) {
        String customerId = ensureCustomer(organizationId);
        String resolvedReturn = blankToDefault(returnUrl, appBaseUrl + "/settings/billing");
        try {
            com.stripe.model.billingportal.Session portal = com.stripe.model.billingportal.Session.create(
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(customerId)
                            .setReturnUrl(resolvedReturn)
                            .build()
            );
            return portal.getUrl();
        } catch (StripeException ex) {
            throw new DomainException("BILLING_ERROR", "Stripe portal failed: " + ex.getMessage());
        }
    }

    @Override
    public List<InvoiceSummary> listInvoices(UUID organizationId, int limit) {
        String customerId = ensureCustomer(organizationId);
        int cappedLimit = Math.min(Math.max(limit, 1), 24);
        try {
            return com.stripe.model.Invoice.list(
                    InvoiceListParams.builder()
                            .setCustomer(customerId)
                            .setLimit((long) cappedLimit)
                            .build()
            ).getData().stream()
                    .map(invoice -> new InvoiceSummary(
                            invoice.getId(),
                            invoice.getNumber(),
                            invoice.getStatus(),
                            invoice.getAmountDue() == null ? 0L : invoice.getAmountDue(),
                            invoice.getCurrency() == null ? "usd" : invoice.getCurrency(),
                            invoice.getCreated(),
                            invoice.getHostedInvoiceUrl(),
                            invoice.getInvoicePdf()
                    ))
                    .toList();
        } catch (StripeException ex) {
            throw new DomainException("BILLING_ERROR", "Stripe invoice list failed: " + ex.getMessage());
        }
    }

    @Override
    public long sumPaidInvoiceCents(
            UUID organizationId,
            long periodStartEpochSeconds,
            long periodEndEpochSeconds
    ) {
        String customerId = ensureCustomer(organizationId);
        try {
            return com.stripe.model.Invoice.list(
                    InvoiceListParams.builder()
                            .setCustomer(customerId)
                            .setLimit(100L)
                            .build()
            ).getData().stream()
                    .filter(invoice -> "paid".equals(invoice.getStatus()))
                    .filter(invoice -> invoice.getCreated() != null
                            && invoice.getCreated() >= periodStartEpochSeconds
                            && invoice.getCreated() < periodEndEpochSeconds)
                    .mapToLong(invoice -> invoice.getAmountPaid() == null ? 0L : invoice.getAmountPaid())
                    .sum();
        } catch (StripeException ex) {
            throw new DomainException("BILLING_ERROR", "Stripe invoice sum failed: " + ex.getMessage());
        }
    }

    @Override
    public void refreshSubscriptionItems(UUID organizationId, String externalSubscriptionId) {
        if (externalSubscriptionId == null || externalSubscriptionId.isBlank()) {
            return;
        }
        OrganizationSubscriptionEntity sub = subscriptionRepository.findByOrganizationId(organizationId)
                .orElse(null);
        if (sub == null) {
            return;
        }
        try {
            Subscription subscription = Subscription.retrieve(externalSubscriptionId);
            applySubscriptionItemIds(sub, subscription);
            subscriptionRepository.save(sub);
        } catch (StripeException ex) {
            throw new DomainException("BILLING_ERROR", "Stripe subscription retrieve failed: " + ex.getMessage());
        }
    }

    @Override
    public MeteredUsageSyncResult reportMeteredUsage(
            UUID organizationId,
            long seatQuantity,
            long aiOverageQuantity,
            long timestampEpochSeconds
    ) {
        OrganizationSubscriptionEntity sub = subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Subscription not found"));
        if (sub.getExternalSubscriptionId() == null || sub.getExternalSubscriptionId().isBlank()) {
            return MeteredUsageSyncResult.skipped("No Stripe subscription for org " + organizationId);
        }
        PlanEntity plan = planRepository.findById(sub.getPlanCode())
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Plan not found"));
        if (sub.getStripeSeatSubscriptionItemId() == null || sub.getStripeAiOverageSubscriptionItemId() == null) {
            refreshSubscriptionItems(organizationId, sub.getExternalSubscriptionId());
            sub = subscriptionRepository.findByOrganizationId(organizationId).orElse(sub);
        }
        List<String> details = new ArrayList<>();
        boolean anySynced = false;
        try {
            if (sub.getStripeSeatSubscriptionItemId() != null
                    && !sub.getStripeSeatSubscriptionItemId().isBlank()
                    && resolveSeatMeteredPriceId(plan) != null) {
                UsageRecord.createOnSubscriptionItem(
                        sub.getStripeSeatSubscriptionItemId(),
                        UsageRecordCreateOnSubscriptionItemParams.builder()
                                .setQuantity(seatQuantity)
                                .setTimestamp(timestampEpochSeconds)
                                .setAction(UsageRecordCreateOnSubscriptionItemParams.Action.SET)
                                .build()
                );
                details.add("seats=" + seatQuantity);
                anySynced = true;
            }
            if (sub.getStripeAiOverageSubscriptionItemId() != null
                    && !sub.getStripeAiOverageSubscriptionItemId().isBlank()
                    && aiOverageQuantity > 0
                    && resolveAiOverageMeteredPriceId(plan) != null) {
                UsageRecord.createOnSubscriptionItem(
                        sub.getStripeAiOverageSubscriptionItemId(),
                        UsageRecordCreateOnSubscriptionItemParams.builder()
                                .setQuantity(aiOverageQuantity)
                                .setTimestamp(timestampEpochSeconds)
                                .setAction(UsageRecordCreateOnSubscriptionItemParams.Action.INCREMENT)
                                .build()
                );
                details.add("aiOverage+=" + aiOverageQuantity);
                anySynced = true;
            }
        } catch (StripeException ex) {
            return MeteredUsageSyncResult.failed("Stripe usage record failed: " + ex.getMessage());
        }
        if (!anySynced) {
            return MeteredUsageSyncResult.skipped("Metered price IDs or subscription items not configured");
        }
        sub.setStripeMeteredUsageSyncedAt(Instant.ofEpochSecond(timestampEpochSeconds));
        subscriptionRepository.save(sub);
        return MeteredUsageSyncResult.synced(String.join(", ", details));
    }

    private String ensureCustomer(UUID organizationId) {
        OrganizationSubscriptionEntity sub = subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Subscription not found"));
        if (sub.getExternalCustomerId() != null && !sub.getExternalCustomerId().isBlank()) {
            return sub.getExternalCustomerId();
        }
        try {
            Customer customer = Customer.create(
                    CustomerCreateParams.builder()
                            .putMetadata("organizationId", organizationId.toString())
                            .setDescription("AI Studio org " + organizationId)
                            .build()
            );
            sub.setExternalCustomerId(customer.getId());
            subscriptionRepository.save(sub);
            return customer.getId();
        } catch (StripeException ex) {
            throw new DomainException("BILLING_ERROR", "Stripe customer create failed: " + ex.getMessage());
        }
    }

    private void applySubscriptionItemIds(OrganizationSubscriptionEntity sub, Subscription subscription) {
        PlanEntity plan = planRepository.findById(sub.getPlanCode()).orElse(null);
        if (plan == null || subscription.getItems() == null) {
            return;
        }
        String basePriceId = resolveBasePriceId(plan);
        String seatPriceId = resolveSeatMeteredPriceId(plan);
        String aiPriceId = resolveAiOverageMeteredPriceId(plan);
        for (SubscriptionItem item : subscription.getItems().getData()) {
            if (item.getPrice() == null) {
                continue;
            }
            String priceId = item.getPrice().getId();
            if (basePriceId != null && priceId.equals(basePriceId)) {
                sub.setStripeBaseSubscriptionItemId(item.getId());
            }
            if (seatPriceId != null && priceId.equals(seatPriceId)) {
                sub.setStripeSeatSubscriptionItemId(item.getId());
            }
            if (aiPriceId != null && priceId.equals(aiPriceId)) {
                sub.setStripeAiOverageSubscriptionItemId(item.getId());
            }
        }
    }

    private String resolveBasePriceId(PlanEntity plan) {
        if (plan.getStripePriceId() != null && !plan.getStripePriceId().isBlank()) {
            return plan.getStripePriceId();
        }
        BillingProperties.Stripe stripe = billingProperties.stripe();
        if (plan.getCode() == PlanCode.PRO) {
            return stripe.proPriceId();
        }
        if (plan.getCode() == PlanCode.TEAM) {
            return stripe.teamPriceId();
        }
        return null;
    }

    private String resolveSeatMeteredPriceId(PlanEntity plan) {
        if (plan.getStripeSeatMeteredPriceId() != null && !plan.getStripeSeatMeteredPriceId().isBlank()) {
            return plan.getStripeSeatMeteredPriceId();
        }
        BillingProperties.Stripe stripe = billingProperties.stripe();
        if (plan.getCode() == PlanCode.PRO) {
            return stripe.proSeatMeteredPriceId();
        }
        if (plan.getCode() == PlanCode.TEAM) {
            return stripe.teamSeatMeteredPriceId();
        }
        return null;
    }

    private String resolveAiOverageMeteredPriceId(PlanEntity plan) {
        if (plan.getStripeAiOverageMeteredPriceId() != null && !plan.getStripeAiOverageMeteredPriceId().isBlank()) {
            return plan.getStripeAiOverageMeteredPriceId();
        }
        BillingProperties.Stripe stripe = billingProperties.stripe();
        if (plan.getCode() == PlanCode.PRO) {
            return stripe.proAiOverageMeteredPriceId();
        }
        if (plan.getCode() == PlanCode.TEAM) {
            return stripe.teamAiOverageMeteredPriceId();
        }
        return null;
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
