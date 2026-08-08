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
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams.LineItem;
import com.stripe.param.InvoiceListParams;
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
        String priceId = resolvePriceId(plan);
        if (priceId == null || priceId.isBlank()) {
            throw new DomainException(
                    "CONFIG_ERROR",
                    "Stripe price ID not configured for plan " + planCode.name()
            );
        }
        String customerId = ensureCustomer(organizationId);
        String resolvedSuccess = blankToDefault(successUrl, appBaseUrl + "/settings/billing?checkout=success");
        String resolvedCancel = blankToDefault(cancelUrl, appBaseUrl + "/settings/billing?checkout=cancel");

        try {
            com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.create(
                    SessionCreateParams.builder()
                            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                            .setCustomer(customerId)
                            .setSuccessUrl(resolvedSuccess)
                            .setCancelUrl(resolvedCancel)
                            .setClientReferenceId(organizationId.toString())
                            .putMetadata("organizationId", organizationId.toString())
                            .putMetadata("planCode", planCode.name())
                            .addLineItem(LineItem.builder().setPrice(priceId).setQuantity(1L).build())
                            .build()
            );
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
    public List<BillingPort.InvoiceSummary> listInvoices(UUID organizationId, int limit) {
        String customerId = ensureCustomer(organizationId);
        int cappedLimit = Math.min(Math.max(limit, 1), 24);
        try {
            return com.stripe.model.Invoice.list(
                    InvoiceListParams.builder()
                            .setCustomer(customerId)
                            .setLimit((long) cappedLimit)
                            .build()
            ).getData().stream()
                    .map(invoice -> new BillingPort.InvoiceSummary(
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

    private String resolvePriceId(PlanEntity plan) {
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

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
